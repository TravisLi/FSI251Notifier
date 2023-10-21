package com.kohang.fsi251notifier.azure;

import com.kohang.fsi251notifier.exception.InvalidDriveItemException;
import com.kohang.fsi251notifier.util.Util;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.requests.DriveItemCollectionPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
public class CloudFileCopier {

    private final AzureFileAccesser azureFileAccesser;
    private final OneDriveFileAccesser oneDriveFileAccesser;
    
    public void copyAllOneDriveCertsToAzureSrcDrive() {
        log.info("Copying all One Drive cert file to Azure src drive");
        DriveItemCollectionPage page = oneDriveFileAccesser.getDriveItemCollectionPageFromRootFolder();
        if (page != null) {
            processDriveItemCollectionPage(page, null);
        } else {
            log.warn("No root folder found!!!");
        }
    }

    public void copyAllOneDriveCertsToAzureSrcDriveWithCreateDate(LocalDate createDate) {
        log.info("Copying all One Drive cert file to azure src drive with create date:" + createDate);

        DriveItemCollectionPage page = oneDriveFileAccesser.getDriveItemCollectionPageFromRootFolder();
        if (page != null) {
            processDriveItemCollectionPage(page, createDate);
        } else {
            log.warn("No root folder found!!!");
        }
    }

    private void processDriveItemCollectionPage(final DriveItemCollectionPage page, LocalDate createDate) {

        page.getCurrentPage().forEach(driveItem -> {
            //it is a folder
            if (driveItem.folder != null) {
                handleFolder(createDate, driveItem);
            //it is a file
            } else {
                handleFile(createDate, driveItem);
            }
        });

        if (page.getNextPage() != null) {
            DriveItemCollectionPage nextPage = page.getNextPage().buildRequest().get();
            if (nextPage != null) {
                processDriveItemCollectionPage(nextPage, createDate);
            }
        }
    }

    private void handleFile(LocalDate createDate, DriveItem driveItem) {
        if (driveItem.name != null && driveItem.name.contains(Util.PDF_EXTENSION)) {
            log.info("processing file with name: " + driveItem.name);

            boolean doUpload;

            if (createDate == null) {
                doUpload = handleFileWithoutCreationDate(driveItem);
            } else {
                doUpload = handleFileWithCreationDate(createDate, driveItem);
            }

            if (doUpload) {
                this.uploadDriveItem(driveItem);
            }
        }
    }

    private boolean handleFileWithCreationDate(LocalDate createDate, DriveItem driveItem) {

        boolean doUpload = false;

        //set the range date range only insert certs start from today with the same month
        //use createDateTime if for daily import
        OffsetDateTime fromOffsetDateTime = createDate.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime toOffsetDateTime = createDate.atStartOfDay(ZoneOffset.UTC).plusDays(1).toOffsetDateTime();

        log.debug("file filter start date:" + fromOffsetDateTime);
        log.debug("file filter end date:" + toOffsetDateTime);

        if (driveItem.createdDateTime != null) {

            log.debug("createdDateTime:" + toOffsetDateTime);

            if ((driveItem.createdDateTime.isAfter(fromOffsetDateTime) || driveItem.createdDateTime.isEqual(fromOffsetDateTime)) && driveItem.createdDateTime.isBefore(toOffsetDateTime)) {
                doUpload = true;
            }
        }
        return doUpload;
    }

    private boolean handleFileWithoutCreationDate(DriveItem driveItem) {

        boolean doUpload = false;

        //set the date range only insert certs start from last year with last month
        //use last modified date if the createDate is null
        LocalDate today = LocalDate.now();
        OffsetDateTime fromOffsetDateTime = today.atStartOfDay(ZoneOffset.UTC).minusYears(1).minusMonths(1).withDayOfMonth(1).toOffsetDateTime();
        OffsetDateTime toOffsetDateTime = today.atStartOfDay(ZoneOffset.UTC).plusDays(1).toOffsetDateTime();

        log.debug("file filter start date:" + fromOffsetDateTime);
        log.debug("file filter end date:" + toOffsetDateTime);

        if (driveItem.lastModifiedDateTime != null) {
            log.debug("lastModifiedDateTime:" + driveItem.lastModifiedDateTime);
            if ((driveItem.lastModifiedDateTime.isAfter(fromOffsetDateTime) || driveItem.lastModifiedDateTime.isEqual(fromOffsetDateTime)) && driveItem.lastModifiedDateTime.isBefore(toOffsetDateTime)) {
                doUpload = true;
            }
        }
        return doUpload;
    }

    private void handleFolder(LocalDate createDate, DriveItem driveItem) {
        log.info("processing folder with name: " + driveItem.name);
        if (driveItem.webUrl != null) {
            DriveItemCollectionPage folderPage = oneDriveFileAccesser.getDriverItemCollectionPage(driveItem);
            if (folderPage != null) {
                processDriveItemCollectionPage(folderPage, createDate);
            }
        }
    }

    private void uploadDriveItem(DriveItem item) {
        log.info("Uploading file to azure");
        try(InputStream is = oneDriveFileAccesser.getInputStreamFromDriveItem(item)){
            azureFileAccesser.uploadToSrcFolder(item.name, item.size, is);
        } catch (InvalidDriveItemException e) {
            log.error(e.getMessage(),e);
        } catch (IOException e) {
            log.error("Error occurs while handling the input steam from drive item");
        }

    }

}

