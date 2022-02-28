package com.kohang.fsi251notifier.email;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.kohang.fsi251notifier.azure.FileAccesser;
import com.kohang.fsi251notifier.model.FSI251Data;
import com.kohang.fsi251notifier.repository.FSI251Repository;
import com.kohang.fsi251notifier.util.Util;

@Component
public class EmailSender {

	private static final Logger logger = LoggerFactory.getLogger(EmailSender.class); 
	private static final String EMAIL_SERVER = "smtp.gmail.com";
	private static final String OFFICE_EMAIL = "office@kohang.com.hk";
	private static final String HTML_TEMLPLATE = "<html>%s</html>";
	private static final String MESSAGE = "以下證書快將到期，請聯絡客戶做年檢</br>";
	private static final String EMAIL_SUBJECT = "證書到期提示";
	private static final String TESTING_KEY_WORD = "<測試>";
	private static final String HTML_TABLE_TEMPLATE = "<table>%s</table>";
	private static final String HTML_TABLE_HEAD_TEMPLATE = "<tr><th>證書號碼</th><th>證書到期日</th></tr>";
	private static final String HTML_TABLE_ROW_TEMPLATE = "<tr><td>%s</td><td>%s</td></tr>";
	private final String username;
	private final String password;
	private final String env;
	
	private FileAccesser fileAccesser;
	private FSI251Repository repository;

	@Autowired
	public EmailSender(@Value("${email_username}")String username,@Value("${email_password}")String password, @Value("${spring.profiles.active}")String env, FileAccesser f, FSI251Repository r) {
		this.username = username;
		this.password = password;
		this.env = env;
		this.fileAccesser = f;
		this.repository = r;
	}

	public void run() throws MessagingException {
		
		LocalDate today = LocalDate.now();
		LocalDate startDate = LocalDate.of(today.getYear(), today.getMonth(), 1);
		LocalDate endDate = LocalDate.of(today.getYear(), today.getMonth(), today.lengthOfMonth());
		
		List<FSI251Data> dataList = repository.findByDateRange(Util.formatLocalDate(startDate), Util.formatLocalDate(endDate));
		
		dataList.stream().forEach(e->logger.info(e.toString()));
		
		/*try {
			send(dataList);
		} catch (MessagingException e) {
			logger.error("Email cannot be sent");
			throw e;
		}*/
		
	}
	
	private void send(List<FSI251Data> list) throws MessagingException {

		Properties prop = new Properties();
		prop.put("mail.smtp.auth", "true");
		prop.put("mail.smtp.starttls.enable", "true");
		prop.put("mail.smtp.host", EMAIL_SERVER);
		prop.put("mail.smtp.port", "587");

		final Session session = Session.getInstance(prop, null);

		Message message = new MimeMessage(session);
		
		List<File> fileList = new LinkedList<File>();
		
		try {
			message.setFrom(new InternetAddress(username));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(OFFICE_EMAIL));
			message.setSubject(env!=null && env.equals("prog")?EMAIL_SUBJECT:EMAIL_SUBJECT+TESTING_KEY_WORD);

			StringBuilder builder = new StringBuilder();
			builder.append(MESSAGE);
			builder.append(buildHTMLTable(list));

			MimeBodyPart mimeBodyPart = new MimeBodyPart();
			mimeBodyPart.setContent(String.format(HTML_TEMLPLATE, builder.toString()), "text/html; charset=utf-8");

			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(mimeBodyPart);

			for(FSI251Data data: list) {
				
				try {
					
					if(data.getFileName()!=null) {
						
						logger.info("Preparing attachment for " + data.getFileName());
												
						String tmpdir = System.getProperty("java.io.tmpdir");

						logger.debug(tmpdir);
						
						File file = new File(tmpdir + File.separator + data.getFileName());
						
						Files.write(file.toPath(), fileAccesser.getProcessedFileByteArrayOutputStream(data.getFileName()).toByteArray());
						
						MimeBodyPart attachmentBodyPart = new MimeBodyPart();
						attachmentBodyPart.attachFile(file);
						
						multipart.addBodyPart(attachmentBodyPart);
						
						fileList.add(file);
					}
					
					
				} catch (IOException e) {
					e.printStackTrace();
					logger.error("Attachement Preparation Error");
				}
			}

			message.setContent(multipart);

			Transport.send(message, username, password);

		} catch (MessagingException e) {
			e.printStackTrace();
			logger.error("Email Message Prepare Exception");
			throw e;
		} finally {
			
			for(File file: fileList) {
				logger.debug("Deleting file " + file.getAbsolutePath() + " result:" + file.delete());
			}
			
		}

	}

	private String buildHTMLTable(List<FSI251Data> list) {

		StringBuilder builder = new StringBuilder();
		builder.append(HTML_TABLE_HEAD_TEMPLATE);

		for(FSI251Data data: list) {

			LocalDate certDate;
			try {
				certDate = Util.convertDateStrToLocalDate(data.getCertDate());
				
				if(certDate!=null) {
					builder.append(String.format(HTML_TABLE_ROW_TEMPLATE, data.getCertNo(), Util.formatLocalDate(Util.getCertExpiryDate(certDate))));
				}
				
			} catch (Exception e) {
				logger.error("Cert Date Conversion Error");
				e.printStackTrace();
			} 
			
		}

		return String.format(HTML_TABLE_TEMPLATE, builder.toString());
	}

}
