package com.kohang.fsi251notifier.azure;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.requests.DriveItemCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

@Component
public class OneDriveFileAccesser {

    private static Logger logger = LoggerFactory.getLogger(OneDriveFileAccesser.class);

    private static final String DEFAULT_SCOPE = "https://graph.microsoft.com/.default";

    private static final String PDF_EXTENSION = ".pdf";

    private final String ENCODED_ONE_DRIVE_SHARE_URL;

    private final GraphServiceClient graphClient;

    public OneDriveFileAccesser(@Value("${azure_client_id}")String clientId, @Value("${azure_client_secret}")String clientSecret, @Value("${azure_tenant_id}")String tenantId, @Value("${onedrive_share_url}")String onedriveShareUrl){

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

    public List<DriveItem> getAllDriveItemsInRootFolder() {
        logger.info("Getting drive items from root folder");
        DriveItemCollectionPage page = graphClient.shares(ENCODED_ONE_DRIVE_SHARE_URL).driveItem().children().buildRequest().get();
        return processDriveItemCollectionPage(page);
    }

    public List<DriveItem> getAllDriveItemsInRootFolderWithCreateDate(LocalDate createDate){
        logger.info("Getting drive items from root folder with create date:" + createDate.toString());
        DriveItemCollectionPage page = graphClient.shares(ENCODED_ONE_DRIVE_SHARE_URL).driveItem().children().buildRequest().get();
        return processDriveItemCollectionPageWithCreateDate(page,createDate);
    }

    private List<DriveItem> processDriveItemCollectionPageWithCreateDate(DriveItemCollectionPage page, LocalDate createDate){

        List<DriveItem> result = new LinkedList<DriveItem>();

        if(page.getCurrentPage()!=null){
            page.getCurrentPage().forEach(driveItem -> {
                //it is a folder
                if(driveItem.folder!=null){
                    logger.info("processing folder with name: " + driveItem.name);
                    DriveItemCollectionPage folderPage = graphClient.shares(encodeURLinBase64Format(driveItem.webUrl)).driveItem().children().buildRequest().get();
                    result.addAll(processDriveItemCollectionPageWithCreateDate(folderPage,createDate));
                    //it is a file
                }else{
                    logger.info("processing file with name: " + driveItem.name);

                    OffsetDateTime fromOffsetDateTime = createDate.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
                    OffsetDateTime toOffsetDateTime = createDate.atStartOfDay(ZoneOffset.UTC).minusDays(-1).toOffsetDateTime();

                    logger.debug("fromOffSetDateTime:" + fromOffsetDateTime.toString());
                    logger.debug("toOffSetDateTime:" + toOffsetDateTime.toString());

                    if(driveItem.name.contains(PDF_EXTENSION)) {
                        if((driveItem.createdDateTime.isAfter(fromOffsetDateTime) || driveItem.createdDateTime.isEqual(fromOffsetDateTime)) && driveItem.createdDateTime.isBefore(toOffsetDateTime)){
                            logger.info("This item is created within the date");
                            result.add(driveItem);
                        }
                    }
                }
            });
        }

        if(page.getNextPage()!=null){
            DriveItemCollectionPage nextPage = page.getNextPage().buildRequest().get();
            if(nextPage!=null){
                result.addAll(processDriveItemCollectionPageWithCreateDate(nextPage,createDate));
            }

        }

        return result;
    }

    private List<DriveItem> processDriveItemCollectionPage(DriveItemCollectionPage page){

        List<DriveItem> result = new LinkedList<DriveItem>();

        if(page.getCurrentPage()!=null){
            page.getCurrentPage().forEach(driveItem -> {
                //it is a folder
                if(driveItem.folder!=null){

                    logger.info("processing folder with name: " + driveItem.name);

                    DriveItemCollectionPage folderPage = graphClient.shares(encodeURLinBase64Format(driveItem.webUrl)).driveItem().children().buildRequest().get();
                    result.addAll(processDriveItemCollectionPage(folderPage));
                    //it is a file
                }else{

                    logger.info("processing file with name: " + driveItem.name);

                    if(driveItem.name.contains(PDF_EXTENSION)) {
                        result.add(driveItem);
                    }
                }
            });
        }

        if(page.getNextPage()!=null){
            DriveItemCollectionPage nextPage = page.getNextPage().buildRequest().get();
            if(nextPage!=null){
                result.addAll(processDriveItemCollectionPage(nextPage));
            }

        }

        return result;
    }

    public List<File> getFilesByDriveItems(List<DriveItem> driveItemList){

        List<File> fileList = new LinkedList<File>();

        driveItemList.stream().forEach(driveItem -> {

            File file = this.getFileByDriveItem(driveItem);

            if(file!=null){
                fileList.add(file);
            }

        });

        return fileList;
    }

    private File getFileByDriveItem(DriveItem driveItem){

        logger.info("Saving file from following url: " + driveItem.webUrl);

        String fileName = driveItem.name;

        String endcodedUrl = encodeURLinBase64Format(driveItem.webUrl);

        InputStream stream = (InputStream) graphClient.shares(endcodedUrl).driveItem().content().buildRequest().get();

        String tmpdir = System.getProperty("java.io.tmpdir");

        File file = new File(tmpdir + File.separator + fileName);

        try {
            Files.copy(stream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
            if(file.exists()){
                file.delete();
            }
            return null;
        }

        return file;
    }

    private static String encodeURLinBase64Format(String url){
        String base64Value = Base64.getEncoder().withoutPadding().encodeToString(url.getBytes(StandardCharsets.UTF_8));
        return "u!" + base64Value.replace('/', '_').replace('+','-');
    }

}