FROM openjdk:17-jdk-alpine
COPY /target/fsi251-notifier-0.0.5-SNAPSHOT.jar /app/
CMD java -jar \
    -Dazure.client.id="$(cat /run/secrets/azure_client_id)" \
    -Dazure.client.secret="$(cat /run/secrets/azure_client_secret)" \
    -Dazure.tenant.id="$(cat /run/secrets/azure_tenant_id)" \
    -Dazure.recognition.endpoint="$(cat /run/secrets/azure_recognition_endpoint)" \
    -Dazure.recognition.key="$(cat /run/secrets/azure_recognition_key)" \
    -Dazure.storage="$(cat /run/secrets/azure_storage)" \
    -Ddb.user="$(cat /run/secrets/db_user)" \
    -Ddb.password="$(cat /run/secrets/db_password)" \
    -Demail.username="$(cat /run/secrets/email_username)" \
    -Demail.password="$(cat /run/secrets/email_password)" \
    -Dweb.user="$(cat /run/secrets/web_user)" \
    -Dweb.password="$(cat /run/secrets/web_password)" \
    -Donedrive.share.url="$(cat /run/secrets/onedrive_share_url)" \
    -Dweb.password="$(cat /run/secrets/web_password)" \
    /app/fsi251-notifier-0.0.5-SNAPSHOT.jar