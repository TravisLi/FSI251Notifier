package com.kohang.fsi251notifier.email;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;

import com.kohang.fsi251notifier.azure.AzureFileAccesser;
import com.kohang.fsi251notifier.model.FSI251Data;
import com.kohang.fsi251notifier.repository.FSI251Repository;
import com.kohang.fsi251notifier.util.TestUtil;

@SpringBootTest(classes= {EmailSenderUnitTest.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EmailSenderUnitTest {

	@Value("${email_username}")
	private String username;
	@Value("${email_password}")
	private String password;
	
	@Mock
	private AzureFileAccesser azureFileAccesser;
	
	@Mock
	private FSI251Repository repository;
	
	@Autowired
	private ResourceLoader resourceLoader;
	
	@BeforeAll
	public void init() {

		try {
			
			ByteArrayOutputStream sampleFileBaos = new ByteArrayOutputStream();
			File sampleFile = resourceLoader.getResource("classpath:" + TestUtil.SAMPLE_FILE).getFile();
			sampleFileBaos.writeBytes(Files.readAllBytes(sampleFile.toPath()));
			
			ByteArrayOutputStream sampleFile1Baos = new ByteArrayOutputStream();
			File sampleFile1 = resourceLoader.getResource("classpath:" + TestUtil.SAMPLE_FILE_1).getFile();
			sampleFile1Baos.writeBytes(Files.readAllBytes(sampleFile1.toPath()));
			
			ByteArrayOutputStream sampleFile2Baos = new ByteArrayOutputStream();
			File sampleFile2 = resourceLoader.getResource("classpath:" + TestUtil.SAMPLE_FILE_2).getFile();
			sampleFile2Baos.writeBytes(Files.readAllBytes(sampleFile2.toPath()));
			
			when(azureFileAccesser.getProcessedFileByteArrayOutputStream(TestUtil.SAMPLE_FILE)).thenReturn(sampleFileBaos);
			when(azureFileAccesser.getProcessedFileByteArrayOutputStream(TestUtil.SAMPLE_FILE_1)).thenReturn(sampleFileBaos);
			when(azureFileAccesser.getProcessedFileByteArrayOutputStream(TestUtil.SAMPLE_FILE_2)).thenReturn(sampleFileBaos);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		List<FSI251Data> list = new ArrayList<FSI251Data>();
		FSI251Data data = new FSI251Data();
		data.setCertNo(TestUtil.SAMPLE_CERT_NO);
		data.setFileName(TestUtil.SAMPLE_FILE);
		data.setCertDate("1/10/2021");
		
		FSI251Data data1 = new FSI251Data();
		data1.setCertNo(TestUtil.SAMPLE_CERT_NO_1);
		data1.setFileName(TestUtil.SAMPLE_FILE_1);
		data1.setCertDate("1/11/2021");
		
		FSI251Data data2 = new FSI251Data();
		data2.setCertNo(TestUtil.SAMPLE_CERT_NO_2);
		data2.setFileName(TestUtil.SAMPLE_FILE_2);
		data2.setCertDate("1/12/2021");
		
		list.add(data);
		list.add(data1);
		list.add(data2);
		
		when(repository.findByDateRange(any(), any())).thenReturn(list);
		
	}
	
	@Test
	public void runTest() {
				
		EmailSender emailSender = new EmailSender(username, password, "test", azureFileAccesser, repository);
		
		assertDoesNotThrow(emailSender::run);
				
	}
	
	
}
