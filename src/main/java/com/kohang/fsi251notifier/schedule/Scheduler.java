package com.kohang.fsi251notifier.schedule;

import com.kohang.fsi251notifier.azure.CloudFileCopier;
import com.kohang.fsi251notifier.azure.FSI251Recognizer;
import com.kohang.fsi251notifier.email.ExceptionEmailSender;
import com.kohang.fsi251notifier.email.Fsi251EmailSender;
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
    private final FSI251Recognizer fsi251Recognizer;
    private final Fsi251EmailSender fsi251EmailSender;
    private final ExceptionEmailSender exceptionEmailSender;

    @Autowired
    public Scheduler(CloudFileCopier c, FSI251Recognizer f, Fsi251EmailSender fes, ExceptionEmailSender ees){
        this.cloudFileCopier = c;
        this.fsi251Recognizer = f;
        this.fsi251EmailSender = fes;
        this.exceptionEmailSender = ees;
    }

    //prod: 0 0 0 * * * execute at 00:00:00 everyday
    @Scheduled(cron = "${import.file.cron}")
    public void importFilesDaily(){
        logger.info("Scheduled file import daily start");
        LocalDate createDate = LocalDate.now();
        cloudFileCopier.copyAllOneDriveCertsToAzureSrcDriveWithCreateDate(createDate);
    }

    //prod: 0 0 1 * * * execute at 00:00 1st of every month
    @Scheduled(cron = "${send.email.cron}")
    public void sendFsi251EmailNotification(){
        logger.info("Scheduled fsi251 email sent start");
        fsi251EmailSender.run();
    }

    //prod: 0 0 1 * * * execute at 01:00:00 everyday
    @Scheduled(cron = "${recognition.cron}")
    public void runRecognition(){
        logger.info("Scheduled recognition start");
        fsi251Recognizer.run();
    }

    //prod: 0 0 2 * * * execute at 01:00:00 everyday
    @Scheduled(cron = "${send.exception.cron}")
    public void runExceptionEmailNotification(){
        logger.info("Scheduled exception email sent start");
        exceptionEmailSender.run();
    }

}
