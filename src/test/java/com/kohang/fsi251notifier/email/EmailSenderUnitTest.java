package com.kohang.fsi251notifier.email;

import com.kohang.fsi251notifier.azure.AzureFileAccesser;
import com.kohang.fsi251notifier.model.FSI251Data;
import com.kohang.fsi251notifier.repository.FSI251Repository;
import com.kohang.fsi251notifier.util.TestUtil;
import com.kohang.fsi251notifier.util.Util;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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

		List<FSI251Data> list = new LinkedList<>();

		IntStream.rangeClosed(1,30).forEach(i->{

			String newFileName = TestUtil.SAMPLE_FILE.replace(Util.PDF_EXTENSION,"_"+i+Util.PDF_EXTENSION);

			FSI251Data data = new FSI251Data();
			data.setCertNo(TestUtil.SAMPLE_CERT_NO + "_" + i);
			data.setFileName(newFileName);
			data.setCertDate(i+"/10/2021");

			try {
				ByteArrayOutputStream sampleFileBaos = new ByteArrayOutputStream();
				File sampleFile = resourceLoader.getResource("classpath:" + TestUtil.SAMPLE_FILE).getFile();
				sampleFileBaos.writeBytes(Files.readAllBytes(sampleFile.toPath()));
				when(azureFileAccesser.getProcessedFileByteArrayOutputStream(newFileName)).thenReturn(sampleFileBaos);
			} catch (IOException e) {
				e.printStackTrace();
			}

			list.add(data);

		});

		when(repository.findByDateRange(any(), any())).thenReturn(list);
		
	}
	
	@Test
	public void runTest() {
				
		EmailSender emailSender = new EmailSender(username, password, "test", azureFileAccesser, repository);
		assertEquals(2,emailSender.run());
	}
	
	
}
