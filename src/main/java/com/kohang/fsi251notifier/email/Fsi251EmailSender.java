package com.kohang.fsi251notifier.email;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.kohang.fsi251notifier.azure.AzureFileAccesser;
import com.kohang.fsi251notifier.model.FSI251Data;
import com.kohang.fsi251notifier.repository.FSI251Repository;
import com.kohang.fsi251notifier.util.Util;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class Fsi251EmailSender extends EmailSender {

    private static final String MESSAGE = "以下證書快將到期，請聯絡客戶做年檢</br>";
    private static final String EMAIL_SUBJECT = "證書到期提示";
    private static final String HTML_TABLE_HEAD_TEMPLATE = "<tr><th>證書號碼</th><th>證書到期日</th></tr>";
    private static final String HTML_TABLE_ROW_TEMPLATE = "<tr><td>%s</td><td>%s</td></tr>";

    private final AzureFileAccesser fileAccesser;
    private final FSI251Repository repository;

    @Autowired
    public Fsi251EmailSender(@Value("${email.username}") String username,
                             @Value("${email.password}") String password,
                             @Value("${spring.profiles.active}") String env, AzureFileAccesser f, FSI251Repository r) {

        super(username,password,env,EMAIL_SUBJECT);
        this.fileAccesser = f;
        this.repository = r;
    }


    public Integer run() {

        //finding those certs with created last year with next month
        //for example today is 1-Mar-2022, we look for the certs created in 1-Apr-2021 to 30-Apr-2021
        LocalDate today = LocalDate.now();
        LocalDate lastYearNextMonth = today.minusYears(1).minusMonths(-1);
        LocalDate startDate = lastYearNextMonth.withDayOfMonth(1);
        LocalDate endDate = lastYearNextMonth.withDayOfMonth(lastYearNextMonth.lengthOfMonth());

		return run(startDate,endDate);

    }

    public Integer run(LocalDate startDate, LocalDate endDate){
        log.info("Finding certs between {} to {}", Util.formatLocalDate(startDate), Util.formatLocalDate(endDate));
        List<FSI251Data> dataList = repository.findByDateRange(Util.formatLocalDate(startDate), Util.formatLocalDate(endDate));

        return processDataList(dataList);
    }

    private void send(List<FSI251Data> dataList, List<File> fileList, String counter) {

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
                    log.error("Attachment Preparation Error", e);
                }

            }

            message.setContent(multipart);
            this.send(message);

        } catch (MessagingException e) {
            log.error("Email Message Prepare Exception",e);
        } finally {

            for (File file : fileList) {
                log.debug("Deleting file {} result: {}", file.getAbsolutePath(), file.delete());
            }
        }
    }

    private Integer processDataList(List<FSI251Data> dataList) {

        List<File> fileList = new LinkedList<>();

        List<FSI251Data> sendList = new LinkedList<>();

        Integer emailCounter = 0;

        long fileSizeCount = 0L;

        for (FSI251Data data : dataList) {

            if (data.getFileName() != null) {

                log.info("Preparing attachment for {}", data.getFileName());

                String tmpdir = System.getProperty("java.io.tmpdir");

                log.debug(tmpdir);

                File file = new File(tmpdir + File.separator + data.getFileName());

                try {

                    Files.write(file.toPath(), fileAccesser.getProcessedFileByteArrayOutputStream(data.getFileName()).toByteArray());

                    log.debug("Total File Size Count: {}",fileSizeCount);
                    log.debug("File Size: {}", file.length());

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
                    log.error("File processing error", e);
                    sendList.add(data);
                }

            }

        }

        //if there are data left need to be sent out
        if(!sendList.isEmpty()){
            emailCounter++;
            this.send(sendList,fileList,emailCounter.toString());
        }

        return emailCounter;
    }

    private String buildHTMLTable(List<FSI251Data> list) {

        StringBuilder builder = new StringBuilder();
        builder.append(HTML_TABLE_HEAD_TEMPLATE);

        for (FSI251Data data : list) {

            LocalDate certDate;
            try {
                certDate = Util.convertDateStrToLocalDate(data.getCertDate());

                builder.append(String.format(HTML_TABLE_ROW_TEMPLATE, data.getCertNo(), Util.formatLocalDate(Util.getCertExpiryDate(certDate))));

            } catch (Exception e) {
                log.error("Cert Date Conversion Error", e);
            }

        }

        return String.format(HTML_TABLE_TEMPLATE, builder);
    }

}
