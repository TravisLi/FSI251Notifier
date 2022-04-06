package com.kohang.fsi251notifier.controller;

import com.kohang.fsi251notifier.azure.AzureFileAccesser;
import com.kohang.fsi251notifier.azure.CloudFileCopier;
import com.kohang.fsi251notifier.azure.FSI251Recognizer;
import com.kohang.fsi251notifier.email.EmailSender;
import com.kohang.fsi251notifier.model.ExceptionData;
import com.kohang.fsi251notifier.repository.ExceptionRepository;
import com.kohang.fsi251notifier.repository.FSI251Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.mail.MessagingException;

@Controller
@RequestMapping("/start")
public class ServiceController {

	private static final Logger logger = LoggerFactory.getLogger(ServiceController.class);

	private final CloudFileCopier copier;
	private final EmailSender sender;
	private final FSI251Recognizer recognizer;
	private final AzureFileAccesser fileAccesser;
	private final FSI251Repository fsi251Repo;
	private final ExceptionRepository exceptionRepo;

	@Autowired
	public ServiceController(CloudFileCopier copier, EmailSender sender, FSI251Recognizer recognizer, AzureFileAccesser fileAccesser, FSI251Repository fsi251Repo, ExceptionRepository exceptionRepo) {
		this.copier = copier;
		this.sender = sender;
		this.recognizer = recognizer;
		this.fileAccesser = fileAccesser;
		this.fsi251Repo = fsi251Repo;
		this.exceptionRepo = exceptionRepo;
	}

	@GetMapping("/import")
	public String importFiles(@AuthenticationPrincipal User user, Model model){
		logger.info("Import files start");

		new Thread(()->{
			fileAccesser.deleteAllFilesInSrcFolder();
			fileAccesser.deleteAllFilesInProcessedFolder();
			fsi251Repo.deleteAll();
			exceptionRepo.deleteAll();
			copier.copyAllOneDriveCertsToAzureSrcDrive();
		}).start();

		model.addAttribute("user",user);
		model.addAttribute("msg","File import started");

		return "manual";
	}

	@GetMapping("/email")
	public String sendEmail(@AuthenticationPrincipal User user, Model model){
		logger.info("Send email start");

		new Thread(sender::run).start();

		model.addAttribute("user",user);
		model.addAttribute("msg","Email send started");

		return "manual";
	}

	@GetMapping("/recognize")
	public String recognize(@AuthenticationPrincipal User user, Model model){
		logger.info("Recognition start");

		new Thread(recognizer::run).start();

		model.addAttribute("user",user);
		model.addAttribute("msg","Recognition started");

		return "manual";
	}


}
