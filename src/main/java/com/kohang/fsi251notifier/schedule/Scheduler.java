package com.kohang.fsi251notifier.schedule;

import com.kohang.fsi251notifier.azure.AzureFileAccesser;
import com.kohang.fsi251notifier.azure.FSI251Recognizer;
import com.kohang.fsi251notifier.azure.OneDriveFileAccesser;
import com.kohang.fsi251notifier.controller.FilesImportController;
import com.kohang.fsi251notifier.email.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import java.io.File;
import java.time.LocalDate;
import java.util.List;

@Component
@EnableScheduling
public class Scheduler {

    private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

    private final AzureFileAccesser azureFileAccesser;
    private final EmailSender emailSender;
    private final FSI251Recognizer fsi251Recognizer;
    private final OneDriveFileAccesser oneDriveFileAccesser;

    @Autowired
    public Scheduler(AzureFileAccesser a, EmailSender e, FSI251Recognizer f, OneDriveFileAccesser o){
        this.azureFileAccesser = a;
        this.emailSender = e;
        this.fsi251Recognizer = f;
        this.oneDriveFileAccesser = o;
    }

    //prod: 0 0 0 * * * execute at 00:00:00 everyday
    @Scheduled(cron = "${import.file.cron}")
    public void importFilesDaily(){
        logger.info("Scheduled file import daily start");
        LocalDate createDate = LocalDate.now();
        List<File> fileList = oneDriveFileAccesser.getFilesByDriveItems(oneDriveFileAccesser.getAllDriveItemsInRootFolderWithCreateDate(createDate));
        azureFileAccesser.uploadToSrcFolder(fileList);
    }

    //prod: 0 0 1 * * *execute at 00:00 1st of every month
    @Scheduled(cron = "${send.email.cron}")
    public void sendEmailNotification(){
        logger.info("Scheduled email Sent Start");
        try {
            emailSender.run();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    //prod: 0 0 1 * * * execute at 01:00:00 everyday
    @Scheduled(cron = "${recognition.cron}")
    public void runRecognition(){
        logger.info("Recognition start");
        fsi251Recognizer.run();
    }

}
