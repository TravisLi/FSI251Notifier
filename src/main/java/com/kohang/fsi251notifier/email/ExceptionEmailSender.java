package com.kohang.fsi251notifier.email;

import com.kohang.fsi251notifier.azure.AzureFileAccesser;
import com.kohang.fsi251notifier.model.ExceptionData;
import com.kohang.fsi251notifier.model.FSI251Data;
import com.kohang.fsi251notifier.repository.ExceptionRepository;
import com.kohang.fsi251notifier.repository.FSI251Repository;
import com.kohang.fsi251notifier.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

@Component
public class ExceptionEmailSender extends EmailSender {

    private static final Logger logger = LoggerFactory.getLogger(Fsi251EmailSender.class);
    private static final String MESSAGE = "以下新增證書有問題，請處理</br>";
    private static final String EMAIL_SUBJECT = "新增證書問題提示";

    private static final String HTML_TABLE_HEAD_TEMPLATE = "<tr><th>證書號碼</th><th>問題</th></tr>";
    private static final String HTML_TABLE_ROW_TEMPLATE = "<tr><td>%s</td><td>%s</td></tr>";
    private final AzureFileAccesser fileAccesser;
    private final ExceptionRepository exceptionRepo;

    @Autowired
    public ExceptionEmailSender(@Value("#{systemProperties['email.username']!=null && systemProperties['email.username']!='' ? systemProperties['email.username'] : systemEnvironment['email_username']}") String username,
                                @Value("#{systemProperties['email.password']!=null && systemProperties['email.password']!='' ? systemProperties['email.password'] : systemEnvironment['email_password']}") String password,
                                @Value("${spring.profiles.active}") String env, AzureFileAccesser f, ExceptionRepository e) {

        super(username, password, env, EMAIL_SUBJECT);
        this.fileAccesser = f;
        this.exceptionRepo = e;
    }

    public Integer run() {
        logger.info("Finding certs has problem");
        List<ExceptionData> dataList = exceptionRepo.findByResolved(false);

        return processDataList(dataList);
    }

    private Integer processDataList(List<ExceptionData> exceptionDataList) {

        List<File> fileList = new LinkedList<>();

        List<ExceptionData> sendList = new LinkedList<>();

        Integer emailCounter = 0;

        long fileSizeCount = 0L;

        for (ExceptionData data : exceptionDataList) {

            FSI251Data fsi251Data = data.getFsi251Data();

            if (fsi251Data != null && fsi251Data.getFileName() != null) {

                logger.info("Preparing attachment for " + fsi251Data.getFileName());

                String tmpdir = System.getProperty("java.io.tmpdir");

                logger.debug(tmpdir);

                File file = new File(tmpdir + File.separator + fsi251Data.getFileName());

                try {

                    Files.write(file.toPath(), fileAccesser.getProcessedFileByteArrayOutputStream(fsi251Data.getFileName()).toByteArray());

                    logger.debug("Total File Size Count:" + fileSizeCount);
                    logger.debug("File Size: " + file.length());

                    if (fileSizeCount + file.length() > MAX_ATTACHMENT_SIZE) {

                        emailCounter++;
                        this.send(sendList, fileList, emailCounter.toString());

                        //reset parameter
                        sendList.clear();
                        fileList.clear();

                        sendList.add(data);
                        fileList.add(file);

                        fileSizeCount = file.length();
                    } else {
                        sendList.add(data);
                        fileList.add(file);
                        fileSizeCount += file.length();
                    }

                } catch (IOException e) {
                    logger.error("File processing error");
                    e.printStackTrace();
                    sendList.add(data);
                }

            }

        }

        //if there are data left need to be sent out
        if (!sendList.isEmpty()) {
            emailCounter++;
            this.send(sendList, fileList, emailCounter.toString());
        }

        return emailCounter;
    }

    private void send(List<ExceptionData> dataList, List<File> fileList, String counter) {

        try {
            Message message = this.createMessage(counter);

            StringBuilder builder = new StringBuilder();
            builder.append(MESSAGE);
            builder.append(buildHTMLTable(dataList));

            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(String.format(HTML_TEMPLATE, builder), "text/html; charset=utf-8");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);

            for (File file : fileList) {

                MimeBodyPart attachmentBodyPart = new MimeBodyPart();

                try {
                    attachmentBodyPart.attachFile(file);
                    multipart.addBodyPart(attachmentBodyPart);
                } catch (IOException e) {
                    logger.error("Attachment Preparation Error");
                    e.printStackTrace();
                }

            }

            message.setContent(multipart);
            this.send(message);

        } catch (MessagingException e) {
            e.printStackTrace();
            logger.error("Email Message Prepare Exception");
        } finally {

            for (File file : fileList) {
                logger.debug("Deleting file " + file.getAbsolutePath() + " result:" + file.delete());
            }
        }
    }

    private static String buildHTMLTable(List<ExceptionData> list) {

        StringBuilder builder = new StringBuilder();
        builder.append(HTML_TABLE_HEAD_TEMPLATE);

        for (ExceptionData exceptionData : list) {

            FSI251Data fsi251Data = exceptionData.getFsi251Data();
            if (fsi251Data != null) {
                builder.append(String.format(HTML_TABLE_ROW_TEMPLATE, fsi251Data.getCertNo(), exceptionData.getRemark()));
            }
        }
        return String.format(HTML_TABLE_TEMPLATE, builder);
    }
}
