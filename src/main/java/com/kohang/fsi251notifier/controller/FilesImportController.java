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
@EnableScheduling
@RequestMapping("/import")
public class FilesImportController {

	private static Logger logger = LoggerFactory.getLogger(FilesImportController.class);

	private final AzureFileAccesser azureFileAccesser;
	private final OneDriveFileAccesser oneDriveFileAccesser;

	@Autowired
	public FilesImportController(AzureFileAccesser a, OneDriveFileAccesser o) {
		this.azureFileAccesser = a;
		this.oneDriveFileAccesser = o;
	}
	
	@GetMapping
	public String importFiles(@AuthenticationPrincipal User user, Model model){
		List<File> fileList = oneDriveFileAccesser.getFilesByDriveItems(oneDriveFileAccesser.getAllDriveItemsInRootFolder());
		List<String> fileNameList = copyFilesToAzureDrive(fileList);
		model.addAttribute("fileList", fileNameList);
		model.addAttribute("user",user);

		return "fileImportList";
	}

	@Scheduled(cron = "${import.file.cron}")
	public void importFilesDaily(){
		logger.info("File Import File Daily Start");
		LocalDate createDate = LocalDate.now();
		List<File> fileList = oneDriveFileAccesser.getFilesByDriveItems(oneDriveFileAccesser.getAllDriveItemsInRootFolderWithCreateDate(createDate));
		copyFilesToAzureDrive(fileList);
	}

	private List<String> copyFilesToAzureDrive(List<File> fileList){

		List<String> fileNameList = new LinkedList<String>();
		fileList.stream().forEach(f->{
			String result = azureFileAccesser.uploadToSrcFolder(f);
			if(!result.isEmpty()){
				fileNameList.add(new String(f.getName()));
			}
			if(f.exists()){
				f.exists();
			}
		});

		return fileNameList;
	}
	
}
