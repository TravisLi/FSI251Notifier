package com.kohang.fsi251notifier.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {

	private static Logger logger = LoggerFactory.getLogger(Util.class); 
	private static final String DATE_DELIMITER = "/";
	public static final String PDF_EXTENSION = ".pdf";
	
	public static LocalDate convertDateStrToLocalDate(String dateStr) throws NumberFormatException, Exception {
		
		String[] dateArray = dateStr.split(DATE_DELIMITER);

		LocalDate certDate = null;

		//make sure the length of year is equal to 4, there is case has year recognized as 20212
		if(dateArray.length==3 && dateArray[2].length()==4) {

			logger.debug(String.format("Day:%s, Month:%s, Year:%s",dateArray[0],dateArray[1],dateArray[2]));

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
