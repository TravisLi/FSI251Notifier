package com.kohang.fsi251notifier.azure;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;

import com.kohang.fsi251notifier.model.FSI251Data;
import com.kohang.fsi251notifier.repository.FSI251Repository;
import com.kohang.fsi251notifier.util.TestUtil;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FSI251RecognizerIntegrationTest {

	@Autowired
	private AzureFileAccesser fileAccesser;

	@Autowired
	private ResourceLoader resourceLoader;
	
	@Autowired
	private FSI251Recognizer fsi251Recognizer;

	@Autowired
	private FSI251Repository fsi251Repo;

	@Autowired
	private TestUtil testUtil;

	@BeforeAll
	void uploadSampleFile() {

		try {

			//prevent too many files copied by One drive file accesser
			testUtil.init();

			File file = resourceLoader.getResource("classpath:" + TestUtil.SAMPLE_FILE).getFile();

			if(file.exists()){
				fileAccesser.uploadToSrcFolder(file);
			}

		} catch (IOException e) {
			fail("Failed to upload sample file to azure: " + e.getMessage());
		}

	}
	
	@Test
	void testRun() {
		List<FSI251Data> dataList = fsi251Recognizer.run();
		
		FSI251Data expect = null;
		
		for(FSI251Data data: dataList) {
			
			if(data.getCertNo().equals(TestUtil.SAMPLE_CERT_NO)) {
				expect = data;
			}	
		}
		
		assertNotNull(expect);

		FSI251Data data = fsi251Repo.findByCertNo(expect.getCertNo());

		assertNotNull(data);
	}

}
