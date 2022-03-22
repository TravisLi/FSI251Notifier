package com.kohang.fsi251notifier.controller;

import com.kohang.fsi251notifier.azure.AzureFileAccesser;
import com.kohang.fsi251notifier.azure.OneDriveFileAccesser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.File;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

@Controller
@RequestMapping("/import")
public class FilesImportController {

	private static final Logger logger = LoggerFactory.getLogger(FilesImportController.class);

	private final AzureFileAccesser azureFileAccesser;
	private final OneDriveFileAccesser oneDriveFileAccesser;

	@Autowired
	public FilesImportController(AzureFileAccesser a, OneDriveFileAccesser o) {
		this.azureFileAccesser = a;
		this.oneDriveFileAccesser = o;
	}
	
	@GetMapping
	public String importFiles(@AuthenticationPrincipal User user, Model model){
		logger.info("Import All File Start");
		List<File> fileList = oneDriveFileAccesser.getFilesByDriveItems(oneDriveFileAccesser.getAllDriveItemsInRootFolder());
		List<String> fileNameList = azureFileAccesser.uploadToSrcFolder(fileList);
		model.addAttribute("fileList", fileNameList);
		model.addAttribute("user",user);
		return "fileImportList";
	}




}
