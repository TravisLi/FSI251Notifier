package com.kohang.fsi251notifier.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class UtilTest {

	@Test
	@DisplayName("Test ConvertDateStrToLocalDate")
	void testConvertDateStr() {

		String testDateStr = "1/1/2021";

		try {

			LocalDate date = Util.convertDateStrToLocalDate(testDateStr);
			assertEquals(1, date.getDayOfMonth());
			assertEquals(Month.JANUARY, date.getMonth());
			assertEquals(2021, date.getYear());

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

	}
	
	@Test
	@DisplayName("Test ConvertDateStrToLocalDate Fail")
	void testConvertDateStrFail() {
		
		//mal format
		//wrong data type with good format
		Stream.of("abcd", "1-1-2021", "99/99/2021", "ab/01/2021").forEach(str ->
			assertThrows(Exception.class,()->Util.convertDateStrToLocalDate(str))
		);

	}
	
}
