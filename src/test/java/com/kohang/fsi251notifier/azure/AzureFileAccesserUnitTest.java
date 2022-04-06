package com.kohang.fsi251notifier.azure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;

import com.azure.storage.file.share.models.ShareStorageException;
import com.kohang.fsi251notifier.util.TestUtil;

@SpringBootTest(classes = {AzureFileAccesser.class})
@TestMethodOrder(OrderAnnotation.class)
public class AzureFileAccesserUnitTest {

	private static final Logger logger = LoggerFactory.getLogger(AzureFileAccesser.class);

	@Autowired
	private AzureFileAccesser fileAccesser;
	
	@Autowired
	private ResourceLoader resourceLoader;

	@Test
	@Order(1)
	public void testUploadFile() {
		
		try {
			
			File file = resourceLoader.getResource("classpath:" + TestUtil.SAMPLE_FILE).getFile();			
			assertFalse(fileAccesser.uploadToSrcFolder(file).isEmpty());
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	@Order(2)
	public void testGetSrcFiles() {
		
		assertTrue(fileAccesser.getSrcFiles().contains(TestUtil.SAMPLE_FILE));
		
	}
	
	@Test
	@Order(3)
	public void testGetSrcFileByeArrayInputStream() {
				
		assertTrue(fileAccesser.getSrcFileByteArrayOutputStream(TestUtil.SAMPLE_FILE).size()>0);
		
	}
	
	@Test
	@Order(4)
	public void testCopyAndDeleteFiles() {
		
		List<String> nameList = new ArrayList<>();
		nameList.add(TestUtil.SAMPLE_FILE);
		
		fileAccesser.copyAndDeleteFiles(nameList);
		
		assertFalse(fileAccesser.getSrcFiles().contains(TestUtil.SAMPLE_FILE));
		
		assertTrue(fileAccesser.getProcessedFileUrl(TestUtil.SAMPLE_FILE).contains(TestUtil.SAMPLE_FILE));
		
	}
	
	@Test
	@Order(5)
	public void testGetProcessedFileByeArrayInputStream() {
				
		assertTrue(fileAccesser.getProcessedFileByteArrayOutputStream(TestUtil.SAMPLE_FILE).size()>0);
		
	}
	
	@Test
	@Order(6)
	public void testGetFileNotFound() {
		
		Exception exception = assertThrows(ShareStorageException.class, () -> fileAccesser.getProcessedFileByteArrayOutputStream(TestUtil.NO_FILE));
		
		String expectedMessage = "ResourceNotFound";
	    String actualMessage = exception.getMessage();
		
	    assertTrue(actualMessage.contains(expectedMessage));
		
	}

	@Test
	@Order(7)
	public void testAllFilesInSrcFolderAreDelete() {

		try {
			File file = resourceLoader.getResource("classpath:" + TestUtil.SAMPLE_FILE).getFile();
			assertFalse(fileAccesser.uploadToSrcFolder(file).isEmpty());

			fileAccesser.deleteAllFilesInSrcFolder();

			assertTrue(fileAccesser.getSrcFiles().isEmpty());

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Test
	@Order(8)
	public void testAllFilesInProcessedFolderAreDelete() {

		try {
			File file = resourceLoader.getResource("classpath:" + TestUtil.SAMPLE_FILE).getFile();
			assertFalse(fileAccesser.uploadToProcessedFolder(file).isEmpty());

			fileAccesser.deleteAllFilesInProcessedFolder();

			assertTrue(fileAccesser.getProcessedFiles().isEmpty());

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}


