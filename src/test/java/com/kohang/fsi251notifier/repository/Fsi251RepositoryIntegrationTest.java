package com.kohang.fsi251notifier.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;

import com.kohang.fsi251notifier.config.TestcontainersConfiguration;
import com.kohang.fsi251notifier.model.FSI251Data;
import com.kohang.fsi251notifier.util.TestUtil;
import com.kohang.fsi251notifier.util.Util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@DataMongoTest
@TestMethodOrder(OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(TestcontainersConfiguration.class)
class Fsi251RepositoryIntegrationTest {

	@Autowired
	private FSI251Repository repository;

	@BeforeAll
	void init() {
		log.info("Test Preparation Start");

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
	void deleteInsertAndRead() {

		log.info("Test findByCertNo Start");

		FSI251Data ret = repository.findByCertNo(TestUtil.SAMPLE_CERT_NO_1);

		assertEquals(TestUtil.SAMPLE_CERT_NO_1, ret.getCertNo());

	}

	@Test
	@DisplayName("Test findByDateRange")
	@Order(2)
	void selectByRange() {

		log.info("Test selectByRange Start");

        List<FSI251Data> list = repository.findByDateRange("1/12/2021", "31/12/2021");

        for (FSI251Data d : list) {
            LocalDate certDate;
            certDate = Util.convertDateStrToLocalDate(d.getCertDate());
            assertEquals(Month.DECEMBER, certDate.getMonth());
        }

	}
}
