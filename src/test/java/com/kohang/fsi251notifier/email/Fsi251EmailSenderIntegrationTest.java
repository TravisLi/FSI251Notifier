package com.kohang.fsi251notifier.email;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

import com.kohang.fsi251notifier.azure.FSI251Recognizer;
import com.kohang.fsi251notifier.repository.FSI251Repository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;

import com.kohang.fsi251notifier.azure.AzureFileAccesser;
import com.kohang.fsi251notifier.util.TestUtil;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Fsi251EmailSenderIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(Fsi251EmailSenderIntegrationTest.class);

	@Autowired
	private Fsi251EmailSender emailSender;
	
	@Autowired
	private AzureFileAccesser fileAccesser;

	@Autowired
	private FSI251Recognizer recognizer;

	@Autowired
	private ResourceLoader resourceLoader;

	@Autowired
	private TestUtil testUtil;

	@BeforeAll
	public void uploadSampleFile() {

		try {

			testUtil.init();

			logger.info("Uploading file for test");
			File file = resourceLoader.getResource("classpath:" + TestUtil.SAMPLE_FILE).getFile();
			File file1 = resourceLoader.getResource("classpath:" + TestUtil.SAMPLE_FILE_1).getFile();	
			File file2 = resourceLoader.getResource("classpath:" + TestUtil.SAMPLE_FILE_2).getFile();	
			fileAccesser.uploadToSrcFolder(file);
			fileAccesser.uploadToSrcFolder(file1);
			fileAccesser.uploadToSrcFolder(file2);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
		
	@Test
	public void run() {

		//make the upload available in DB
		recognizer.run();

		//those sample has cert date in Dec-2021
		LocalDate startDate = LocalDate.of(2021,12,1);
		LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

		assertEquals(1,emailSender.run(startDate,endDate));

	}
	
}
