package com.kohang.fsi251notifier.azure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;

import com.azure.storage.file.share.models.ShareStorageException;
import com.kohang.fsi251notifier.util.TestUtil;

@SpringBootTest(classes = {FileAccesser.class})
@TestMethodOrder(OrderAnnotation.class)
public class FileAccesserTest {
		
	@Autowired
	private FileAccesser fileAccesser;
	
	@Autowired
	private ResourceLoader resourceLoader;
	
	@Test
	@Order(1)
	public void testUploadFile() {
		
		try {
			
			File file = resourceLoader.getResource(TestUtil.SAMPLE_FILE).getFile();			
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
		
		List<String> nameList = new ArrayList<String>();
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
		
		Exception exception = assertThrows(ShareStorageException.class, () ->{
			fileAccesser.getProcessedFileByteArrayOutputStream(TestUtil.NO_FILE);
		});
		
		String expectedMessage = "ResourceNotFound";
	    String actualMessage = exception.getMessage();
		
	    assertTrue(actualMessage.contains(expectedMessage));
		
	}
	
}


