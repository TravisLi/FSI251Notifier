package com.kohang.fsi251notifier.azure;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.requests.DriveItemCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Component
public class OneDriveFileAccesser {

    private static final Logger logger = LoggerFactory.getLogger(OneDriveFileAccesser.class);

    private static final String DEFAULT_SCOPE = "https://graph.microsoft.com/.default";

    private static final String PDF_EXTENSION = ".pdf";

    private final String ENCODED_ONE_DRIVE_SHARE_URL;

    private final GraphServiceClient graphClient;

    public OneDriveFileAccesser(@Value("${azure_client_id}") String clientId, @Value("${azure_client_secret}") String clientSecret, @Value("${azure_tenant_id}") String tenantId, @Value("${onedrive_share_url}") String onedriveShareUrl) {

        this.ENCODED_ONE_DRIVE_SHARE_URL = encodeURLinBase64Format(onedriveShareUrl);

        final List<String> SCOPES = Arrays.asList(DEFAULT_SCOPE);

        final ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .tenantId(tenantId)
                .build();

        final TokenCredentialAuthProvider tokenCredAuthProvider =
                new TokenCredentialAuthProvider(SCOPES, clientSecretCredential);

        this.graphClient = GraphServiceClient
                .builder()
                .authenticationProvider(tokenCredAuthProvider)
                .buildClient();
    }

    public DriveItemCollectionPage getDriveItemCollectionPageFromRootFolder(){
        return graphClient.shares(ENCODED_ONE_DRIVE_SHARE_URL).driveItem().children().buildRequest().get();
    }

    public DriveItemCollectionPage getDriverItemCollectionPage(DriveItem item){
        return graphClient.shares(encodeURLinBase64Format(item.webUrl)).driveItem().children().buildRequest().get();
    }

    public InputStream getInputStreamFromDriveItem(DriveItem driveItem) {

        if (driveItem != null && driveItem.webUrl != null) {

            logger.info("Getting input stream from following url: " + driveItem.webUrl);

            try{
                return graphClient.shares(encodeURLinBase64Format(driveItem.webUrl)).driveItem().content().buildRequest().get();
            }catch(ClientException e){
                logger.error("Getting error while getting input stream");
                e.printStackTrace();
            }
        }

        return null;
    }

    /*public List<DriveItem> getAllDriveItemsInRootFolder() {
        logger.info("Getting drive items from root folder");

        List<DriveItem> resultList = new LinkedList<>();

        DriveItemCollectionPage page = graphClient.shares(ENCODED_ONE_DRIVE_SHARE_URL).driveItem().children().buildRequest().get();

        if (page != null) {
            resultList = processDriveItemCollectionPage(page);
        }

        return resultList;
    }

    public List<DriveItem> getAllDriveItemsInRootFolderWithCreateDate(LocalDate createDate) {
        logger.info("Getting drive items from root folder with create date:" + createDate.toString());

        List<DriveItem> resultList = new LinkedList<>();

        DriveItemCollectionPage page = graphClient.shares(ENCODED_ONE_DRIVE_SHARE_URL).driveItem().children().buildRequest().get();

        if (page != null) {
            resultList = processDriveItemCollectionPageWithCreateDate(page, createDate);
        }

        return resultList;
    }*/

    /*private List<DriveItem> processDriveItemCollectionPageWithCreateDate(DriveItemCollectionPage page, LocalDate createDate) {

        List<DriveItem> result = new LinkedList<>();

        OffsetDateTime fromOffsetDateTime = createDate.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime toOffsetDateTime = createDate.atStartOfDay(ZoneOffset.UTC).minusDays(-1).toOffsetDateTime();

        logger.debug("fromOffSetDateTime:" + fromOffsetDateTime);
        logger.debug("toOffSetDateTime:" + toOffsetDateTime);

        page.getCurrentPage().forEach(driveItem -> {
            //it is a folder
            if (driveItem.folder != null) {
                logger.info("processing folder with name: " + driveItem.name);
                if (driveItem.webUrl != null) {
                    DriveItemCollectionPage folderPage = graphClient.shares(encodeURLinBase64Format(driveItem.webUrl)).driveItem().children().buildRequest().get();
                    if (folderPage != null) {
                        result.addAll(processDriveItemCollectionPageWithCreateDate(folderPage, createDate));
                    }
                }
                //it is a file
            } else {
                logger.info("processing file with name: " + driveItem.name);
                if (driveItem.name != null) {
                    if (driveItem.name.contains(PDF_EXTENSION)) {
                        logger.info("Drive item create date: " + driveItem.createdDateTime);
                        if (driveItem.createdDateTime != null) {
                            if ((driveItem.createdDateTime.isAfter(fromOffsetDateTime) || driveItem.createdDateTime.isEqual(fromOffsetDateTime)) && driveItem.createdDateTime.isBefore(toOffsetDateTime)) {
                                logger.info("This item is created within the date");
                                result.add(driveItem);
                            }
                        }
                    }
                }
            }
        });


        if (page.getNextPage() != null) {
            DriveItemCollectionPage nextPage = page.getNextPage().buildRequest().get();
            if (nextPage != null) {
                result.addAll(processDriveItemCollectionPageWithCreateDate(nextPage, createDate));
            }

        }

        return result;
    }*/

    /*private List<DriveItem> processDriveItemCollectionPage(DriveItemCollectionPage page) {

        List<DriveItem> result = new LinkedList<>();

        page.getCurrentPage().forEach(driveItem -> {
            //it is a folder
            if (driveItem.folder != null) {

                logger.info("processing folder with name: " + driveItem.name);
                if (driveItem.webUrl != null) {
                    DriveItemCollectionPage folderPage = graphClient.shares(encodeURLinBase64Format(driveItem.webUrl)).driveItem().children().buildRequest().get();
                    if (folderPage != null) {
                        result.addAll(processDriveItemCollectionPage(folderPage));
                    }
                }
                //it is a file
            } else {

                if (driveItem.name != null) {
                    logger.info("processing file with name: " + driveItem.name);

                    if (driveItem.name.contains(PDF_EXTENSION)) {
                        result.add(driveItem);
                    }
                }
            }
        });

        if (page.getNextPage() != null) {
            DriveItemCollectionPage nextPage = page.getNextPage().buildRequest().get();
            if (nextPage != null) {
                result.addAll(processDriveItemCollectionPage(nextPage));
            }

        }

        return result;
    }*/

    /*public List<File> getFilesByDriveItems(List<DriveItem> driveItemList) {

        List<File> fileList = new LinkedList<>();

        driveItemList.forEach(driveItem -> {

            File file = this.getFileByDriveItem(driveItem);

            if (file != null) {
                fileList.add(file);
            }

        });

        return fileList;
    }*/

    /*public  File getFileByDriveItem(DriveItem driveItem) {

        if (driveItem.name != null && driveItem.webUrl != null) {

            logger.info("Saving file from following url: " + driveItem.webUrl);

            String fileName = driveItem.name;

            InputStream stream = graphClient.shares(encodeURLinBase64Format(driveItem.webUrl)).driveItem().content().buildRequest().get();

            if(stream!=null){

                File file = new File(System.getProperty("java.io.tmpdir") + File.separator + fileName);

                try {
                    Files.copy(stream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    logger.error("Cannot get the file from one driver");
                    e.printStackTrace();
                    if (file.exists()) {
                        if(file.delete()){
                            logger.info("File is successfully deleted from system");
                        }else{
                            logger.warn("File cannot be deleted from system");
                        }
                    }
                    return null;
                }

                return file;

            }

        }

        return null;
    }*/

    private static String encodeURLinBase64Format(String url) {
        String base64Value = Base64.getEncoder().withoutPadding().encodeToString(url.getBytes(StandardCharsets.UTF_8));
        return "u!" + base64Value.replace('/', '_').replace('+', '-');
    }

}