package com.kohang.fsi251notifier.respository;

import com.kohang.fsi251notifier.model.ExceptionData;
import com.kohang.fsi251notifier.model.FSI251Data;
import com.kohang.fsi251notifier.repository.ExceptionRepository;
import com.kohang.fsi251notifier.repository.FSI251Repository;
import com.kohang.fsi251notifier.util.TestUtil;
import com.kohang.fsi251notifier.util.Util;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureDataMongo
public class ExceptionRepositoryTest {

	private static final Logger logger = LoggerFactory.getLogger(ExceptionRepositoryTest.class);

	@Autowired
	private ExceptionRepository repository;

	@BeforeAll
	@Transactional
	public void init() {
		logger.debug("Test Preparation Start");

		repository.deleteAll();

		FSI251Data data = new FSI251Data();
		data.setCertNo(TestUtil.SAMPLE_CERT_NO);
		data.setFileName(TestUtil.SAMPLE_FILE);
		data.setCertDate(TestUtil.SAMPLE_CERT_DATE);

		FSI251Data data1 = new FSI251Data();
		data1.setCertNo(TestUtil.SAMPLE_CERT_NO_1);
		data1.setFileName(TestUtil.SAMPLE_FILE_1);
		data1.setCertDate(TestUtil.SAMPLE_CERT_DATE_1);

		FSI251Data data2 = new FSI251Data();
		data2.setCertNo(TestUtil.SAMPLE_CERT_NO_2);
		data2.setFileName(TestUtil.SAMPLE_FILE_2);
		data2.setCertDate(TestUtil.SAMPLE_CERT_DATE_2);

		ExceptionData exception1 = new ExceptionData(data, "Testing");
		ExceptionData exception2 = new ExceptionData(data1, "Testing");
		ExceptionData exception3 = new ExceptionData(data2, "Testing", true);

		repository.save(exception1);
		repository.save(exception2);
		repository.save(exception3);
	}

	@Test
	@DisplayName("Test findByNotResolved")
	@Order(1)
	public void findByNotResolved() {

		List<ExceptionData> list = repository.findByResolved(false);

		list.forEach(e->
				assertFalse(e.getResolved())
		);

	}

}
