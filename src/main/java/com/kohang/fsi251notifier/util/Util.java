package com.kohang.fsi251notifier.util;

import lombok.extern.slf4j.Slf4j;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
public class Util {

	private static final String DATE_DELIMITER = "/";
	public static final String PDF_EXTENSION = ".pdf";

	private Util(){}

	public static LocalDate convertDateStrToLocalDate(String dateStr) {
		
		String[] dateArray = dateStr.split(DATE_DELIMITER);

		LocalDate certDate;

		//make sure the length of year is equal to 4, there is case has year recognized as 20212
		if(dateArray.length==3 && dateArray[2].length()==4) {

			log.debug(String.format("Day:%s, Month:%s, Year:%s",dateArray[0],dateArray[1],dateArray[2]));

			certDate = LocalDate.of(Integer.parseInt(dateArray[2]), Integer.parseInt(dateArray[1]), Integer.parseInt(dateArray[0]));

		}else {
			throw new DateTimeException("Cert Date format must be dd/mm/yyyy");
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
