package com.kohang.fsi251notifier.azure;

import com.azure.storage.file.share.models.ShareStorageException;
import com.kohang.fsi251notifier.util.TestUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest(classes = {AzureFileAccesser.class})
@TestMethodOrder(OrderAnnotation.class)
class AzureFileAccesserUnitTest {

	@Autowired
	private AzureFileAccesser fileAccesser;
	
	@Autowired
	private ResourceLoader resourceLoader;

	@Test
	@Order(1)
	void testUploadFile() {
		
		try {
			
			File file = resourceLoader.getResource("classpath:" + TestUtil.SAMPLE_FILE).getFile();
			assertFalse(fileAccesser.uploadToSrcFolder(file).isEmpty());
			
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
	}
	
	@Test
	@Order(2)
	void testGetSrcFiles() {
		
		assertTrue(fileAccesser.getSrcFiles().contains(TestUtil.SAMPLE_FILE));
		
	}
	
	@Test
	@Order(3)
	void testGetSrcFileByeArrayInputStream() {
				
		assertTrue(fileAccesser.getSrcFileByteArrayOutputStream(TestUtil.SAMPLE_FILE).size()>0);
		
	}
	
	@Test
	@Order(4)
	void testCopyAndDeleteFiles() {
		
		List<String> nameList = new ArrayList<>();
		nameList.add(TestUtil.SAMPLE_FILE);
		
		fileAccesser.copyAndDeleteFiles(nameList);
		
		assertFalse(fileAccesser.getSrcFiles().contains(TestUtil.SAMPLE_FILE));
		
		assertTrue(fileAccesser.getProcessedFileUrl(TestUtil.SAMPLE_FILE).contains(TestUtil.SAMPLE_FILE));
		
	}
	
	@Test
	@Order(5)
	void testGetProcessedFileByeArrayInputStream() {
				
		assertTrue(fileAccesser.getProcessedFileByteArrayOutputStream(TestUtil.SAMPLE_FILE).size()>0);
		
	}
	
	@Test
	@Order(6)
	void testGetFileNotFound() {
		
		Exception exception = assertThrows(ShareStorageException.class, () -> fileAccesser.getProcessedFileByteArrayOutputStream(TestUtil.NO_FILE));
		
		String expectedMessage = "ResourceNotFound";
	    String actualMessage = exception.getMessage();
		
	    assertTrue(actualMessage.contains(expectedMessage));
		
	}

	@Test
	@Order(7)
	void testAllFilesInSrcFolderAreDelete() {

		try {
			File file = resourceLoader.getResource("classpath:" + TestUtil.SAMPLE_FILE).getFile();
			assertFalse(fileAccesser.uploadToSrcFolder(file).isEmpty());

			fileAccesser.deleteAllFilesInSrcFolder();

			assertTrue(fileAccesser.getSrcFiles().isEmpty());

		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}

	}

	@Test
	@Order(8)
	void testAllFilesInProcessedFolderAreDelete() {

		try {
			File file = resourceLoader.getResource("classpath:" + TestUtil.SAMPLE_FILE).getFile();
			assertFalse(fileAccesser.uploadToProcessedFolder(file).isEmpty());

			fileAccesser.deleteAllFilesInProcessedFolder();

			assertTrue(fileAccesser.getProcessedFiles().isEmpty());

		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}

	}

}