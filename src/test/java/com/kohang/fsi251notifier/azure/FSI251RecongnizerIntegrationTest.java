package com.kohang.fsi251notifier.azure;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.kohang.fsi251notifier.repository.FSI251Repository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;

import com.kohang.fsi251notifier.model.FSI251Data;
import com.kohang.fsi251notifier.util.TestUtil;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FSI251RecongnizerIntegrationTest {

	@Autowired
	private AzureFileAccesser fileAccesser;

	@Autowired
	private ResourceLoader resourceLoader;
	
	@Autowired
	private FSI251Recognizer fsi251Recognizer;

	@Autowired
	private FSI251Repository repository;

	@BeforeAll
	public void uploadSampleFile() {

		try {

			repository.deleteAll();

			File file = resourceLoader.getResource("classpath:" + TestUtil.SAMPLE_FILE).getFile();			
			fileAccesser.uploadToSrcFolder(file);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	@Test
	public void testRun() {
		List<FSI251Data> dataList = fsi251Recognizer.run();
		
		FSI251Data expect = null;
		
		for(FSI251Data data: dataList) {
			
			if(data.getCertNo().equals(TestUtil.SAMPLE_CERT_NO)) {
				expect = data;
			}	
		}
		
		assertNotNull(expect);

		FSI251Data data = repository.findByCertNo(expect.getCertNo());

		assertNotNull(data);
	}

}
