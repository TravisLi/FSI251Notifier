package com.kohang.fsi251notifier.azure;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.stereotype.Component;

import com.kohang.fsi251notifier.exception.InvalidDriveItemException;
import com.kohang.fsi251notifier.util.Util;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.requests.DriveItemCollectionPage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CloudFileCopier {

    private final AzureFileAccesser azureFileAccesser;
    private final OneDriveFileAccesser oneDriveFileAccesser;
    
    public int copyAllOneDriveCertsToAzureSrcDrive() {
        log.info("Copying all One Drive cert file to Azure src drive");
        DriveItemCollectionPage page = oneDriveFileAccesser.getDriveItemCollectionPageFromRootFolder();
        if (page != null) {
            return processDriveItemCollectionPage(page, null);
        } else {
            log.warn("No root folder found!!!");
            return 0;
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

    private int processDriveItemCollectionPage(DriveItemCollectionPage page, LocalDate createDate) {

        int count = page.getCurrentPage().stream()
            .mapToInt(driveItem -> {
                if (driveItem.folder != null) {
                    return handleFolder(createDate, driveItem);
                } else {
                    return handleFile(createDate, driveItem);
                }
            })
            .sum();

        if (page.getNextPage() != null) {
            var nextPageRequest = page.getNextPage();
            if (nextPageRequest != null) {
                DriveItemCollectionPage nextPage = nextPageRequest.buildRequest().get();
                if (nextPage != null) {
                    count += processDriveItemCollectionPage(nextPage, createDate);
                }
            }
        }

        return count;
    }

    private int handleFile(LocalDate createDate, DriveItem driveItem) {
        String fileName = driveItem.name;
        if (fileName != null && fileName.contains(Util.PDF_EXTENSION)) {
            log.info("processing file with name: " + fileName);

            boolean doUpload;

            if (createDate == null) {
                doUpload = handleFileWithoutCreationDate(driveItem);
            } else {
                doUpload = handleFileWithCreationDate(createDate, driveItem);
            }

            if (doUpload) {
                this.uploadDriveItem(driveItem);
                return 1;
            }
        }
        return 0;
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

            log.debug("createdDateTime:" + driveItem.createdDateTime);

            OffsetDateTime createdDateTime = driveItem.createdDateTime;
            if (createdDateTime != null && (createdDateTime.isAfter(fromOffsetDateTime) || createdDateTime.isEqual(fromOffsetDateTime)) && createdDateTime.isBefore(toOffsetDateTime)) {
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
            OffsetDateTime lastModifiedDateTime = driveItem.lastModifiedDateTime;
            if (lastModifiedDateTime != null && (lastModifiedDateTime.isAfter(fromOffsetDateTime) || lastModifiedDateTime.isEqual(fromOffsetDateTime)) && lastModifiedDateTime.isBefore(toOffsetDateTime)) {
                doUpload = true;
            }
        }
        return doUpload;
    }

    private int handleFolder(LocalDate createDate, DriveItem driveItem) {
        log.info("processing folder with name: " + driveItem.name);
        if (driveItem.webUrl != null) {
            DriveItemCollectionPage folderPage = oneDriveFileAccesser.getDriverItemCollectionPage(driveItem);
            if (folderPage != null) {
                return processDriveItemCollectionPage(folderPage, createDate);
            }
        }
        return 0;
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

