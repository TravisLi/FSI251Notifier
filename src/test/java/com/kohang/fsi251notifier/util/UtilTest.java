package com.kohang.fsi251notifier.util;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.Month;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class UtilTest {

	@Test
	@DisplayName("Test ConvertDateStrToLocalDate")
	public void testConvertDateStr() {
		
		String testDateStr = "1/1/2021";
		
		try {
			
			LocalDate date = Util.convertDateStrToLocalDate(testDateStr);
			assertTrue(date.getDayOfMonth()==1);
			assertTrue(date.getMonth()==Month.JANUARY);
			assertTrue(date.getYear()==2021);
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	@Test
	@DisplayName("Test ConvertDateStrToLocalDate Fail")
	public void testConvertDateStrFail() {
		
		//mal format
		//wrong data type with good format
		
		Stream.of("abcd", "1-1-2021", "99/99/2021", "ab/01/2021").forEach(str -> {
			
			assertThrows(Exception.class,()->Util.convertDateStrToLocalDate(str));
			
		});
				
	}
	
}
