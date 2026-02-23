# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build (skip tests)
mvn -B -DskipTests clean package

# Run unit tests only (*UnitTest.java)
mvn test

# Run all tests including integration tests (*IntegrationTest.java)
# Requires Azure and DB credentials as environment variables (see below)
mvn verify

# Run a single test class
mvn test -Dtest=UtilTest
mvn failsafe:integration-test -Dit.test=FSI251RecognizerIntegrationTest

# Build Docker image
mvn -B -DskipTests clean package docker:build

# Push Docker image
mvn -B -Ddocker.username=<user> -Ddocker.password=<pass> docker:push
```

## Running Locally

Start a local MongoDB for development:
```bash
docker-compose -f docker-dev/mongo/docker-compose.yaml up
```

Run the app with the `dev` profile (set credentials as system properties or env vars):
```bash
java -jar target/fsi251-notifier-1.0.0.jar \
  -Dspring.profiles.active=dev \
  -Dazure.client.id=... \
  -Dazure.client.secret=... \
  -Dazure.tenant.id=... \
  -Dazure.recognition.endpoint=... \
  -Dazure.recognition.key=... \
  -Dazure.storage=... \
  -Ddb.user=... -Ddb.password=... \
  -Demail.username=... -Demail.password=... \
  -Dweb.user=... -Dweb.password=...
```

## Architecture

This is a Spring Boot 2.6 application (Java 17, Maven) that automates FSI-251 fire safety certificate renewal reminders. Certificates are stored as PDFs and their metadata is recognized via Azure Document Intelligence.

### Core Data Flow

1. **File Import** (`CloudFileCopier`): Copies PDF certificates from a shared OneDrive folder → Azure File Share (`fsi251file/source/`)
2. **Recognition** (`FSI251Recognizer`): Reads PDFs from `source/`, splits multi-page PDFs page-by-page, submits each page to Azure Document Intelligence using the custom model `FSI251` (fields: `buildingName`, `clientName`, `certNo`, `certDate`), saves extracted data to MongoDB, moves processed files to `fsi251file/processed/`
3. **Email Notification** (`Fsi251EmailSender`): Queries MongoDB for certs whose `certDate` falls in the same calendar month one year ago (i.e., expiring next month), sends email with HTML table + PDF attachments. Splits into multiple emails if attachments exceed size limit.
4. **Exception Notification** (`ExceptionEmailSender`): Sends email for records in the `ExceptionData` MongoDB collection (documents that failed date parsing during recognition).

All four steps run on Spring `@Scheduled` cron jobs (`Scheduler`) and can also be triggered manually via the web UI (`ServiceController`).

### Package Structure

| Package | Purpose |
|---|---|
| `azure/` | Azure File Share access (`AzureFileAccesser`), OneDrive access (`OneDriveFileAccesser`), recognition (`FSI251Recognizer`), file copying orchestration (`CloudFileCopier`) |
| `email/` | `EmailSender` base class, `Fsi251EmailSender` (renewal notices), `ExceptionEmailSender` (error alerts) |
| `controller/` | `FSI251Controller` (`/fsi251` list view), `ServiceController` (`/start/import`, `/start/recognize`, `/start/email` manual triggers), `AuthenicationController` (login/logout) |
| `schedule/` | `Scheduler` — wires all cron jobs |
| `model/` | `FSI251Data` and `ExceptionData` MongoDB documents |
| `repository/` | Spring Data MongoDB repositories; `FSI251Repository.findByDateRange` uses a raw MongoDB `$expr`/`$dateFromString` query |
| `security/` | In-memory single-user Spring Security (`SecurityConfig`); credentials from system properties or env vars |
| `mongo/` | `MongoConfig` — connection string uses env vars |
| `util/` | `Util` — date parsing (`dd/MM/yyyy`) and cert expiry calculation |

### Configuration & Secrets

All sensitive values are read from either Java system properties (e.g., `-Dazure.client.id`) or environment variables (e.g., `azure_client_id`). The `@Value` expressions check system properties first, then fall back to environment variables.

| Secret | System property | Env var |
|---|---|---|
| Azure AD client ID | `azure.client.id` | `azure_client_id` |
| Azure AD client secret | `azure.client.secret` | `azure_client_secret` |
| Azure AD tenant ID | `azure.tenant.id` | `azure_tenant_id` |
| Document Intelligence endpoint | `azure.recognition.endpoint` | `azure_recognition_endpoint` |
| Document Intelligence key | `azure.recognition.key` | `azure_recognition_key` |
| Azure Storage connection string | `azure.storage` | `azure_storage` |
| MongoDB user | `db.user` | `db_user` |
| MongoDB password | `db.password` | `db_password` |
| Email SMTP username | `email.username` | `email_username` |
| Email SMTP password | `email.password` | `email_password` |
| Web login user | `web.user` | `web_user` |
| Web login password | `web.password` | `web_password` |
| OneDrive share URL | `onedrive.share.url` | `onedrive_share_url` |

### Spring Profiles

- **`dev`**: Cron jobs fire every 10–30 minutes; verbose Azure/Mongo logging enabled
- **`test`**: All cron jobs disabled (set to an unreachable time so scheduled tasks never fire during tests)
- **`prod`**: Production schedule — file import daily at midnight, recognition at 01:00, email on 1st of month at midnight, exception email at 01:30

### Test Conventions

- Files named `*UnitTest.java` are run by maven-surefire (`mvn test`) — no external services needed (uses Mockito)
- Files named `*IntegrationTest.java` are run by maven-failsafe (`mvn verify`) — require real Azure credentials and a live MongoDB instance
- Integration tests for the recognizer (`FSI251RecognizerIntegrationTest`) use test PDF files in `src/main/resources/` (`test_sample.pdf`, `test_sample_1.pdf`, etc.)
- There is a 2-second sleep between each page during recognition to stay within Azure Document Intelligence free-tier rate limits

### Production Deployment

The app runs as a Docker Swarm stack (`docker-compose.yaml`). All secrets are passed via Docker secrets mounted at `/run/secrets/`. MongoDB, the app, and Mongo Express are in the same `app-net` network. The app listens on port 8080 (mapped to 7070 externally).
