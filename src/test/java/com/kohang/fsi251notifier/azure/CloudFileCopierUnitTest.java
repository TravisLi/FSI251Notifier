package com.kohang.fsi251notifier.azure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {AzureFileAccesser.class,OneDriveFileAccesser.class,CloudFileCopier.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CloudFileCopierUnitTest {

    @Autowired
    private CloudFileCopier copier;

    @Autowired
    private AzureFileAccesser fileAccesser;

    @BeforeEach
    void clearFiles(){
        fileAccesser.deleteAllFilesInSrcFolder();
    }

    @Test
    //TODO the result need to be dynamic
    void testCopyAllOneDriveCertsToAzureSrcDrive(){

        copier.copyAllOneDriveCertsToAzureSrcDrive();

        assertEquals(4, fileAccesser.getSrcFiles().size());

    }

    @Test
    void testCopyAllOneDriveCertsToAzureSrcDriveWithCreateDate(){

        LocalDate createDate = LocalDate.of(2022,3,23);

        copier.copyAllOneDriveCertsToAzureSrcDriveWithCreateDate(createDate);

        assertEquals(5, fileAccesser.getSrcFiles().size());

    }

}
