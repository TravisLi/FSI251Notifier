package com.kohang.fsi251notifier.azure;

import com.kohang.fsi251notifier.util.Util;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.requests.DriveItemCollectionPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class CloudFileCopier {

    private static final Logger logger = LoggerFactory.getLogger(CloudFileCopier.class);

    private final AzureFileAccesser azureFileAccesser;
    private final OneDriveFileAccesser oneDriveFileAccesser;

    @Autowired
    public CloudFileCopier(AzureFileAccesser a, OneDriveFileAccesser o) {

        this.azureFileAccesser = a;
        this.oneDriveFileAccesser = o;

    }

    public void copyAllOneDriveCertsToAzureSrcDrive() {
        logger.info("Copying all One Drive cert file to Azure src drive");
        DriveItemCollectionPage page = oneDriveFileAccesser.getDriveItemCollectionPageFromRootFolder();
        if (page != null) {
            processDriveItemCollectionPage(page, null);
        } else {
            logger.warn("No root folder found!!!");
        }
    }

    public void copyAllOneDriveCertsToAzureSrcDriveWithCreateDate(LocalDate createDate) {
        logger.info("Copying all One Drive cert file to azure src drive with create date:" + createDate);

        DriveItemCollectionPage page = oneDriveFileAccesser.getDriveItemCollectionPageFromRootFolder();
        if (page != null) {
            processDriveItemCollectionPage(page, createDate);
        } else {
            logger.warn("No root folder found!!!");
        }
    }

    public void processDriveItemCollectionPage(final DriveItemCollectionPage page, LocalDate createDate) {

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

                if (driveItem.name != null && driveItem.name.contains(Util.PDF_EXTENSION)) {
                    logger.info("processing file with name: " + driveItem.name);

                    boolean doUpload = false;

                    OffsetDateTime fromOffsetDateTime;
                    OffsetDateTime toOffsetDateTime;

                    //set the date range only insert certs start from last year with last month
                    //use last modified date if the create date is null
                    if(createDate==null){
                        LocalDate today = LocalDate.now();
                        fromOffsetDateTime = today.atStartOfDay(ZoneOffset.UTC).minusYears(1).minusMonths(1).withDayOfMonth(1).toOffsetDateTime();
                        toOffsetDateTime = today.atStartOfDay(ZoneOffset.UTC).plusDays(1).toOffsetDateTime();

                        logger.debug("file filter start date:" + fromOffsetDateTime);
                        logger.debug("file filter end date:" + toOffsetDateTime);

                        if (driveItem.lastModifiedDateTime != null) {

                            logger.debug("lastModifiedDateTime:" + toOffsetDateTime);

                            if ((driveItem.lastModifiedDateTime.isAfter(fromOffsetDateTime) || driveItem.lastModifiedDateTime.isEqual(fromOffsetDateTime)) && driveItem.lastModifiedDateTime.isBefore(toOffsetDateTime)) {
                                doUpload = true;
                            }
                        }
                    //set the range date range only insert certs start from today with the same month
                    //use createDateTime if for daily import
                    }else{
                        fromOffsetDateTime = createDate.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
                        toOffsetDateTime = createDate.atStartOfDay(ZoneOffset.UTC).plusDays(1).toOffsetDateTime();

                        logger.debug("file filter start date:" + fromOffsetDateTime);
                        logger.debug("file filter end date:" + toOffsetDateTime);

                        if (driveItem.createdDateTime != null) {

                            logger.debug("createdDateTime:" + toOffsetDateTime);

                            if ((driveItem.createdDateTime.isAfter(fromOffsetDateTime) || driveItem.createdDateTime.isEqual(fromOffsetDateTime)) && driveItem.createdDateTime.isBefore(toOffsetDateTime)) {
                                doUpload = true;
                            }
                        }
                    }



                    if (doUpload) {
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

    private void uploadDriveItem(DriveItem item) {
        InputStream is = oneDriveFileAccesser.getInputStreamFromDriveItem(item);
        if (is != null) {
            azureFileAccesser.uploadToSrcFolder(item.name, item.size, is);
        }
    }

}

