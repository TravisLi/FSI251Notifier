package com.kohang.fsi251notifier.respository;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.kohang.fsi251notifier.model.FSI251Data;
import com.kohang.fsi251notifier.repository.FSI251Repository;
import com.kohang.fsi251notifier.util.TestUtil;
import com.kohang.fsi251notifier.util.Util;

@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
@AutoConfigureDataMongo
public class RepositoryTest {

	private static Logger logger = LoggerFactory.getLogger(RepositoryTest.class);

	@Autowired
	private FSI251Repository repository; 

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

		repository.save(data);
		repository.save(data1);
		repository.save(data2);
	}

	@Test
	@DisplayName("Test findByCertNo")
	@Order(1)
	public void deleteInsertAndRead() {

		FSI251Data ret = repository.findByCertNo(TestUtil.SAMPLE_CERT_NO_1);

		assertTrue(ret.getCertNo().equals(TestUtil.SAMPLE_CERT_NO_1));

	}

	@Test
	@DisplayName("Test findByDateRange")
	@Order(2)
	public void selectByRange() {

		List<FSI251Data> list = repository.findByDateRange("1/12/2021", "31/12/2021");

		for(FSI251Data d:list) {
			LocalDate certDate;
			try {
				certDate = Util.convertDateStrToLocalDate(d.getCertDate());
				assertTrue(certDate.getMonth().equals(Month.DECEMBER));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
}
