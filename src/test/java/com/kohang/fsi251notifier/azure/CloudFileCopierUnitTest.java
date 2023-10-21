package com.kohang.fsi251notifier.azure;

import com.kohang.fsi251notifier.util.Util;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.requests.DriveItemCollectionPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {AzureFileAccesser.class,OneDriveFileAccesser.class,CloudFileCopier.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CloudFileCopierUnitTest {

    @Autowired
    private CloudFileCopier copier;

    @Autowired
    private AzureFileAccesser azureFileAccesser;

    @Autowired
    private OneDriveFileAccesser oneDriveFileAccesser;

    @BeforeAll
    void init(){
        updateOneDriveFilesCreationDate(LocalDate.now());
    }

    @BeforeEach
    void cleanUp(){
        azureFileAccesser.deleteAllFilesInSrcFolder();
    }

    @Test
    //TODO the result need to be dynamic
    void testCopyAllOneDriveCertsToAzureSrcDrive(){

        copier.copyAllOneDriveCertsToAzureSrcDrive();

        assertEquals(4, azureFileAccesser.getSrcFiles().size());

    }

    @Test
    void testCopyAllOneDriveCertsToAzureSrcDriveWithCreateDate(){

        copier.copyAllOneDriveCertsToAzureSrcDriveWithCreateDate(LocalDate.now());

        assertEquals(4, azureFileAccesser.getSrcFiles().size());

    }

    private void updateOneDriveFilesCreationDate(LocalDate dateToUpdate){
        DriveItemCollectionPage page = oneDriveFileAccesser.getDriveItemCollectionPageFromRootFolder();
        if(page!=null){
            processDriveItemCollectionPage(page, dateToUpdate);
        }
    }

    private void processDriveItemCollectionPage(final DriveItemCollectionPage page, final LocalDate dateToUpdate){
        page.getCurrentPage().forEach(driveItem -> {
            //it is a folder
            if (driveItem.folder != null) {
                handleFolder(driveItem, dateToUpdate);
                //it is a file
            } else {
                updateFileCreationAndLastModifiedDate(driveItem, dateToUpdate);
            }
        });

        if (page.getNextPage() != null) {
            DriveItemCollectionPage nextPage = page.getNextPage().buildRequest().get();
            if (nextPage != null) {
                processDriveItemCollectionPage(nextPage, dateToUpdate);
            }
        }
    }

    private void updateFileCreationAndLastModifiedDate(DriveItem driveItem, LocalDate dateToUpdate) {
        if (driveItem.name != null && driveItem.name.contains(Util.PDF_EXTENSION)) {
            driveItem.fileSystemInfo.lastModifiedDateTime = OffsetDateTime.of(dateToUpdate, LocalTime.MIDNIGHT, ZoneOffset.UTC);
            driveItem.fileSystemInfo.createdDateTime = OffsetDateTime.of(dateToUpdate, LocalTime.MIDNIGHT, ZoneOffset.UTC);
            oneDriveFileAccesser.patchDriveItem(driveItem);
        }
    }

    private void handleFolder(DriveItem driveItem, LocalDate dateToUpdate) {
        if (driveItem.webUrl != null) {

            //Intended to skip the folder of 2020 to maintain the last modified date as unchanged
            if(driveItem.name.equals("2020")){
                return;
            }

            DriveItemCollectionPage folderPage = oneDriveFileAccesser.getDriverItemCollectionPage(driveItem);

            if (folderPage != null) {
                processDriveItemCollectionPage(folderPage, dateToUpdate);
            }
        }
    }

}
