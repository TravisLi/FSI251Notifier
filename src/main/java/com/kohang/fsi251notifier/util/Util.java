package com.kohang.fsi251notifier.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {

	private static Logger logger = LoggerFactory.getLogger(Util.class); 
	private static final String DATE_DELIMTER = "/";
	
	public static LocalDate convertDateStrToLocalDate(String dateStr) throws NumberFormatException, Exception {
		
		String[] dateArray = dateStr.split(DATE_DELIMTER);

		LocalDate certDate = null;
		
		if(dateArray.length==3) {

			logger.debug(String.format("Day:%s, Month:%s, Year:%s",dateArray[2],dateArray[1],dateArray[1]));

			certDate = LocalDate.of(Integer.valueOf(dateArray[2]), Integer.valueOf(dateArray[1]), Integer.valueOf(dateArray[0]));

		}else {
			throw new Exception("Cert Date format must be dd/mm/yyyy");
		}
	
		return certDate;
		
	}
	
	public static LocalDate getCertExpiryDate(LocalDate certDate) {
		return certDate.minusYears(-1).minusDays(-1);
	}
	
	public static String formatLocalDate(LocalDate date) {
		return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
	}
	
}
