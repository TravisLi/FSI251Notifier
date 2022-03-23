package com.kohang.fsi251notifier.schedule;

import com.kohang.fsi251notifier.azure.CloudFileCopier;
import com.kohang.fsi251notifier.azure.FSI251Recognizer;
import com.kohang.fsi251notifier.email.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import java.time.LocalDate;

@Component
@EnableScheduling
public class Scheduler {

    private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

    private final CloudFileCopier cloudFileCopier;
    private final EmailSender emailSender;
    private final FSI251Recognizer fsi251Recognizer;

    @Autowired
    public Scheduler(CloudFileCopier c, EmailSender e, FSI251Recognizer f){
        this.cloudFileCopier = c;
        this.emailSender = e;
        this.fsi251Recognizer = f;
    }

    //prod: 0 0 0 * * * execute at 00:00:00 everyday
    @Scheduled(cron = "${import.file.cron}")
    public void importFilesDaily(){
        logger.info("Scheduled file import daily start");
        LocalDate createDate = LocalDate.now();
        cloudFileCopier.copyAllOneDriveCertsToAzureSrcDriveWithCreateDate(createDate);
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
