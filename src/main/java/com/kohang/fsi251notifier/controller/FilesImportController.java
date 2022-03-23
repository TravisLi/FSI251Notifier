package com.kohang.fsi251notifier.controller;

import com.kohang.fsi251notifier.azure.CloudFileCopier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/import")
public class FilesImportController {

	private static final Logger logger = LoggerFactory.getLogger(FilesImportController.class);

	private final CloudFileCopier copier;

	@Autowired
	public FilesImportController(CloudFileCopier c) {
		this.copier = c;
	}
	
	@GetMapping
	public String importFiles(){
		logger.info("Import All File Start");

		new Thread(copier::copyAllOneDriveCertsToAzureSrcDrive).start();

		return "fileImportList";
	}




}
