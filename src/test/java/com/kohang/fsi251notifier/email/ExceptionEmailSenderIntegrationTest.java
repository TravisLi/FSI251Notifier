package com.kohang.fsi251notifier.email;

import com.kohang.fsi251notifier.azure.AzureFileAccesser;
import com.kohang.fsi251notifier.azure.FSI251Recognizer;
import com.kohang.fsi251notifier.util.TestUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExceptionEmailSenderIntegrationTest {

	@Autowired
	private ExceptionEmailSender emailSender;
	
	@Autowired
	private AzureFileAccesser fileAccesser;

	@Autowired
	private FSI251Recognizer recognizer;

	@Autowired
	private ResourceLoader resourceLoader;

	@Autowired
	private TestUtil testUtil;
	
	@BeforeAll
	void uploadSampleFile() {
		try {
			testUtil.init();

			log.info("Uploading file for test");
			File file = resourceLoader.getResource("classpath:" + TestUtil.SAMPLE_FILE_EXCEPTION).getFile();
			fileAccesser.uploadToSrcFolder(file);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
		
	@Test
	void run() {
		//make the upload available in DB
		recognizer.run();
		assertEquals(1,emailSender.run());
	}
	
}
