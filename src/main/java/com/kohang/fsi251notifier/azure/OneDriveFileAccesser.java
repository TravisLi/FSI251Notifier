package com.kohang.fsi251notifier.azure;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.kohang.fsi251notifier.exception.InvalidDriveItemException;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.requests.DriveItemCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class OneDriveFileAccesser {

    private static final String DEFAULT_SCOPE = "https://graph.microsoft.com/.default";

    private final String encodedOneDriveShareUrl;

    private final GraphServiceClient<Request> graphClient;

    public OneDriveFileAccesser(@Value("#{systemProperties['azure.client.id']!=null && systemProperties['azure.client.id']!='' ? systemProperties['azure.client.id'] : systemEnvironment['azure_client_id']}"
    ) String clientId,
                                @Value("#{systemProperties['azure.client.secret']!=null && systemProperties['azure.client.secret']!='' ? systemProperties['azure.client.secret'] : systemEnvironment['azure_client_secret']}"
                                ) String clientSecret,
                                @Value("#{systemProperties['azure.tenant.id']!=null && systemProperties['azure.tenant.id']!='' ? systemProperties['azure.tenant.id'] : systemEnvironment['azure_tenant_id']}"
                                ) String tenantId,
                                @Value("#{systemProperties['onedrive.share.url']!=null && systemProperties['onedrive.share.url']!='' ? systemProperties['onedrive.share.url'] : systemEnvironment['onedrive_share_url']}"
                                ) String onedriveShareUrl) {

        encodedOneDriveShareUrl = encodeURLinBase64Format(onedriveShareUrl.strip());

        final List<String> scopes = Collections.singletonList(DEFAULT_SCOPE);

        final ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
                .clientId(clientId.strip())
                .clientSecret(clientSecret.strip())
                .tenantId(tenantId.strip())
                .build();

        final TokenCredentialAuthProvider tokenCredAuthProvider =
                new TokenCredentialAuthProvider(scopes, clientSecretCredential);

        graphClient = GraphServiceClient
                .builder()
                .authenticationProvider(tokenCredAuthProvider)
                .buildClient();
    }

    public DriveItemCollectionPage getDriveItemCollectionPageFromRootFolder() {
        log.info(encodedOneDriveShareUrl);
        return graphClient.shares(encodedOneDriveShareUrl).driveItem().children().buildRequest().get();
    }

    public DriveItemCollectionPage getDriverItemCollectionPage(DriveItem item) {
        return graphClient.shares(encodeURLinBase64Format(item.webUrl)).driveItem().children().buildRequest().get();
    }

    public InputStream getInputStreamFromDriveItem(DriveItem driveItem) throws InvalidDriveItemException {

        if (driveItem != null && driveItem.webUrl != null) {

            log.info("Getting input stream from following url: {}", driveItem.webUrl);

            try {
                return graphClient.shares(encodeURLinBase64Format(driveItem.webUrl)).driveItem().content().buildRequest().get();
            } catch (ClientException e) {
                log.error("Error occurs while getting input stream", e);
            }
        }

        throw new InvalidDriveItemException("Cannot get input stream from drive item");
    }

    private static String encodeURLinBase64Format(String url) {
        String base64Value = Base64.getEncoder().withoutPadding().encodeToString(url.getBytes(StandardCharsets.UTF_8));
        return "u!" + base64Value.replace('/', '_').replace('+', '-');
    }

}