package com.kohang.fsi251notifier.azure;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.kohang.fsi251notifier.exception.InvalidDriveItemException;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.requests.DriveItemCollectionPage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(classes = {OneDriveFileAccesser.class})
class OneDriveFileAccesserUnitTest {

    @Autowired
    private OneDriveFileAccesser fileAccesser;

    @Test
    void testGetDriveItemCollectionPageFromRootFolder() {

        DriveItemCollectionPage page = fileAccesser.getDriveItemCollectionPageFromRootFolder();

        //there are three folders exist
        assertEquals(3,page.getCurrentPage().size());

    }

    @Test
    void testGetDriveItemCollectionPage() {

        DriveItemCollectionPage page = fileAccesser.getDriveItemCollectionPageFromRootFolder();

        page.getCurrentPage().forEach(item->{
            log.info("drive item name: {}", item.name);
            DriveItemCollectionPage childPage = fileAccesser.getDriverItemCollectionPage(item);
            assertNotNull(childPage.getCurrentPage());
        });

    }

    @Test
    void testGetInputStreamFromDriveItem(){

        DriveItemCollectionPage page = fileAccesser.getDriveItemCollectionPageFromRootFolder();

        page.getCurrentPage().stream().filter(item->"2020".equals(item.name)).forEach(item->{

            log.info("drive item name: {}", item.name);
            DriveItemCollectionPage childPage = fileAccesser.getDriverItemCollectionPage(item);
            DriveItem childItem = childPage.getCurrentPage().get(0);

            assertNotNull(childItem);

            log.info("child drive item name: {}", item.name);

            try(InputStream is = fileAccesser.getInputStreamFromDriveItem(childItem)){
                assertNotNull(is);
            }catch (InvalidDriveItemException | IOException e) {
                fail("Failed to get input stream from drive item: " + childItem.name);
            }

        });

    }

}


