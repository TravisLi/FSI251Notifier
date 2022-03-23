package com.kohang.fsi251notifier.azure;

import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.requests.DriveItemCollectionPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class CloudFileCopier {

    private static final Logger logger = LoggerFactory.getLogger(CloudFileCopier.class);

    private static final String PDF_EXTENSION = ".pdf";
    private final AzureFileAccesser azureFileAccesser;
    private final OneDriveFileAccesser oneDriveFileAccesser;

    @Autowired
    public CloudFileCopier(AzureFileAccesser a, OneDriveFileAccesser o){

        this.azureFileAccesser = a;
        this.oneDriveFileAccesser = o;

    }

    public void copyAllOneDriveCertsToAzureSrcDrive(){
        logger.info("Copying all One Drive cert file to Azure src drive");
        DriveItemCollectionPage page = oneDriveFileAccesser.getDriveItemCollectionPageFromRootFolder();
        if(page!=null){
            processDriveItemCollectionPage(page, null);
        }else{
            logger.warn("No root folder found!!!");
        }
    }

    public void copyAllOneDriveCertsToAzureSrcDriveWithCreateDate(LocalDate createDate){
        logger.info("Copying all One Drive cert file to azure src drive with create date:" + createDate);

        DriveItemCollectionPage page = oneDriveFileAccesser.getDriveItemCollectionPageFromRootFolder();
        if(page!=null){
            processDriveItemCollectionPage(page, createDate);
        }else{
            logger.warn("No root folder found!!!");
        }
    }

    public void processDriveItemCollectionPage(DriveItemCollectionPage page, LocalDate createDate){

        page.getCurrentPage().forEach(driveItem -> {
            //it is a folder
            if (driveItem.folder != null) {

                logger.info("processing folder with name: " + driveItem.name);
                if (driveItem.webUrl != null) {
                    DriveItemCollectionPage folderPage = oneDriveFileAccesser.getDriverItemCollectionPage(driveItem);
                    if (folderPage != null) {
                        processDriveItemCollectionPage(folderPage, createDate);
                    }
                }
                //it is a file
            } else {

                if (driveItem.name != null && driveItem.name.contains(PDF_EXTENSION)) {
                    logger.info("processing file with name: " + driveItem.name);

                    boolean doUpload = true;

                    if(createDate!=null){
                        if (driveItem.createdDateTime != null) {

                            OffsetDateTime fromOffsetDateTime = createDate.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
                            OffsetDateTime toOffsetDateTime = createDate.atStartOfDay(ZoneOffset.UTC).minusDays(-1).toOffsetDateTime();

                            if (!((driveItem.createdDateTime.isAfter(fromOffsetDateTime) || driveItem.createdDateTime.isEqual(fromOffsetDateTime)) && driveItem.createdDateTime.isBefore(toOffsetDateTime))) {
                                doUpload = false;
                            }
                        }
                    }

                    if(doUpload){
                        logger.info("Uploading file to azure");
                        this.uploadDriveItem(driveItem);
                    }

                }
            }
        });

        if (page.getNextPage() != null) {
            DriveItemCollectionPage nextPage = page.getNextPage().buildRequest().get();
            if (nextPage != null) {
                processDriveItemCollectionPage(nextPage, createDate);
            }
        }
    }

    private void uploadDriveItem(DriveItem item){
        InputStream is = oneDriveFileAccesser.getInputStreamFromDriveItem(item);
        if(is!=null){
            azureFileAccesser.uploadToSrcFolder(item.name, item.size, is);
        }
    }

}

