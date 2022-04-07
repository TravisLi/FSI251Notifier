package com.kohang.fsi251notifier.util;

import com.kohang.fsi251notifier.azure.AzureFileAccesser;
import com.kohang.fsi251notifier.model.FSI251Data;
import com.kohang.fsi251notifier.repository.ExceptionRepository;
import com.kohang.fsi251notifier.repository.FSI251Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TestUtil {

	private static final Logger logger = LoggerFactory.getLogger(TestUtil.class);

	public static final String SAMPLE_CERT_NO = "A 7956010";
	public static final String SAMPLE_CERT_NO_1 = "A 7956011";
	public static final String SAMPLE_CERT_NO_2 = "A 7956012";
	public static final String SAMPLE_CERT_NO_EXCEPTION = "7956019";
	public static final String SAMPLE_CERT_DATE = "1/10/2021";
	public static final String SAMPLE_CERT_DATE_1 = "1/11/2021";
	public static final String SAMPLE_CERT_DATE_2 = "1/12/2021";
	public static final String SAMPLE_CERT_DATE_EXCEPTION = "1/1/2021 12334";
	public static final String SAMPLE_FILE = "test_sample.pdf";
	public static final String SAMPLE_FILE_1 = "test_sample_1.pdf";
	public static final String SAMPLE_FILE_2 = "test_sample_2.pdf";
	public static final String SAMPLE_FILE_EXCEPTION = "test_sample_exception.pdf";
	public static final String NO_FILE = "NOFILE.pdf";
	
	public static FSI251Data getFSI251Data(Integer number) {
		
		return switch(number) {
			
		case 1 -> new FSI251Data(SAMPLE_CERT_NO_1, SAMPLE_CERT_DATE_1, SAMPLE_FILE_1);
		case 2 -> new FSI251Data(SAMPLE_CERT_NO_2, SAMPLE_CERT_DATE_2, SAMPLE_FILE_2);
		case 3 -> new FSI251Data(SAMPLE_CERT_NO_EXCEPTION, SAMPLE_CERT_DATE_EXCEPTION, SAMPLE_FILE_EXCEPTION);
		default -> new FSI251Data(SAMPLE_CERT_NO, SAMPLE_CERT_DATE, SAMPLE_FILE);
		
		};
		
	}

	@Autowired
	private FSI251Repository fsi251Repository;
	@Autowired
	private ExceptionRepository exceptionRepository;
	@Autowired
	private AzureFileAccesser azureFileAccesser;

	public void init(){
		this.initAzureStorage();
		this.initDB();
	}

	public void initDB(){
		logger.info("Deleting DB data");
		fsi251Repository.deleteAll();
		exceptionRepository.deleteAll();
	}

	public void initAzureStorage(){
		logger.info("Deleting storage data");
		azureFileAccesser.deleteAllFilesInSrcFolder();
		azureFileAccesser.deleteAllFilesInProcessedFolder();
	}

}
