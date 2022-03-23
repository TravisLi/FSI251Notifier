package com.kohang.fsi251notifier.azure;

import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.requests.DriveItemCollectionPage;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {OneDriveFileAccesser.class})
public class OneDriveFileAccesserUnitTest {

    private static final Logger logger = LoggerFactory.getLogger(OneDriveFileAccesserUnitTest.class);

    @Autowired
    private OneDriveFileAccesser fileAccesser;

    @Test
    public void testGetDriveItemCollectionPageFromRootFolder() {

        DriveItemCollectionPage page = fileAccesser.getDriveItemCollectionPageFromRootFolder();

        //there are three folders exist
        assertEquals(3,page.getCurrentPage().size());

    }

    @Test
    public void testGetDriveItemCollectionPage() {

        DriveItemCollectionPage page = fileAccesser.getDriveItemCollectionPageFromRootFolder();

        page.getCurrentPage().stream().forEach(item->{
            logger.info("drive item name:"+item.name);
            DriveItemCollectionPage childPage = fileAccesser.getDriverItemCollectionPage(item);
            assertNotNull(childPage.getCurrentPage());
        });

    }

    @Test
    public void testGetInputStreamFromDriveItem(){

        DriveItemCollectionPage page = fileAccesser.getDriveItemCollectionPageFromRootFolder();

        page.getCurrentPage().stream().filter(item->item.name.equals("2020")).forEach(item->{

            logger.info("drive item name:"+item.name);
            DriveItemCollectionPage childPage = fileAccesser.getDriverItemCollectionPage(item);
            DriveItem childItem = childPage.getCurrentPage().get(0);

            assertNotNull(childItem);

            logger.info("child drive item name:"+item.name);

            InputStream is = fileAccesser.getInputStreamFromDriveItem(childItem);
            assertNotNull(is);

            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        });

    }

    /*private List<DriveItem> getDriveItemWithTargetedCreateDate(){

        //There are file with the create date of 28/2/2022
        LocalDate createDate = LocalDate.of(2022,2,28);
        return fileAccesser.getAllDriveItemsInRootFolderWithCreateDate(createDate);

    }*/

}


