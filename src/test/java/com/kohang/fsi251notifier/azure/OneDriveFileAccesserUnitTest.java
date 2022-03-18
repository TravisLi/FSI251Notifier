package com.kohang.fsi251notifier.azure;

import com.microsoft.graph.models.DriveItem;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {OneDriveFileAccesser.class})
public class OneDriveFileAccesserUnitTest {

    @Autowired
    private OneDriveFileAccesser fileAccesser;

    @Test
    public void testGetDriveItemFromShareFolder() {

        List<DriveItem> driveItemListList = fileAccesser.getAllDriveItemsInRootFolder();

        assertTrue(driveItemListList.size() > 0);

        assertTrue(driveItemListList.stream().filter(di->!di.name.contains(".pdf")).count()==0);
    }

    @Test
    public void testGetDriveItemFromShareFolderWithCreateDate() {

        assertTrue(getDriveItemWithTargetedCreateDate().size()>0);
    }

    @Test
    public void testGetFileFromDriveItem(){

        List<File> fileList = fileAccesser.getFilesByDriveItems(getDriveItemWithTargetedCreateDate());

        assertTrue(fileList.size()>0);

        fileList.stream().forEach(f->{
            if(f.exists()){
                f.delete();
            }
        });

    }

    private List<DriveItem> getDriveItemWithTargetedCreateDate(){

        //There are file with the create date of 28/2/2022
        LocalDate createDate = LocalDate.of(2022,2,28);
        return fileAccesser.getAllDriveItemsInRootFolderWithCreateDate(createDate);

    }

}


