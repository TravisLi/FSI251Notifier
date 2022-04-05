package com.kohang.fsi251notifier.email;

import com.kohang.fsi251notifier.azure.AzureFileAccesser;
import com.kohang.fsi251notifier.model.ExceptionData;
import com.kohang.fsi251notifier.model.FSI251Data;
import com.kohang.fsi251notifier.repository.ExceptionRepository;
import lombok.extern.slf4j.Slf4j;
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
import java.util.LinkedList;
import java.util.List;

@Slf4j
@Component
public class ExceptionEmailSender extends EmailSender {

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

    public Integer run() {
        log.info("Finding certs has problem");
        List<ExceptionData> dataList = exceptionRepo.findByResolved(false);
        return processExceptionDataList(dataList);
    }

    private Integer processExceptionDataList(List<ExceptionData> exceptionDataList) {

        List<File> fileList = new LinkedList<>();

        List<ExceptionData> sendList = new LinkedList<>();

        Integer emailCounter = 0;

        long fileSizeCount = 0L;

        for (ExceptionData data : exceptionDataList) {

            FSI251Data fsi251Data = data.getFsi251Data();

            if (fsi251Data != null && fsi251Data.getFileName() != null) {

                log.info("Preparing attachment for " + fsi251Data.getFileName());

                String tmpdir = System.getProperty("java.io.tmpdir");

                log.debug(tmpdir);

                File file = new File(tmpdir + File.separator + fsi251Data.getFileName());

                try {

                    Files.write(file.toPath(), fileAccesser.getProcessedFileByteArrayOutputStream(fsi251Data.getFileName()).toByteArray());

                    log.debug("Total File Size Count:" + fileSizeCount);
                    log.debug("File Size: " + file.length());

                    if (fileSizeCount + file.length() > MAX_ATTACHMENT_SIZE) {

                        emailCounter++;
                        send(sendList, fileList, emailCounter.toString());

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
                    log.error("File processing error", e);
                    sendList.add(data);
                }

            }

        }

        //if there are data left need to be sent out
        if (!sendList.isEmpty()) {
            emailCounter++;
            send(sendList, fileList, emailCounter.toString());
        }

        return emailCounter;
    }

    private void send(List<ExceptionData> dataList, List<File> fileList, String counter) {

        try {
            Message message = createMessage(counter);

            StringBuilder builder = new StringBuilder();
            builder.append(MESSAGE);
            builder.append(buildHTMLTable(dataList));

            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(String.format(HTML_TEMPLATE, builder), "text/html; charset=utf-8");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);

            attachFiles(fileList, multipart);

            message.setContent(multipart);
            send(message);

        } catch (MessagingException e) {
            log.error("Email Message Prepare Exception", e);
        } finally {
            for (File file : fileList) {
                try {
                    log.debug("Deleting file " + file.getAbsolutePath() + " result:" + file.delete());
                }catch(Exception e){
                    log.error("Error occurs whiling deleting files", e);
                }
            }
        }
    }

    private void attachFiles(List<File> fileList, Multipart multipart) throws MessagingException {
        for (File file : fileList) {

            MimeBodyPart attachmentBodyPart = new MimeBodyPart();

            try {
                attachmentBodyPart.attachFile(file);
                multipart.addBodyPart(attachmentBodyPart);
            } catch (IOException e) {
                log.error("Attachment Preparation Error", e);
            }

        }
    }
}
