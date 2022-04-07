package com.kohang.fsi251notifier.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailSender {

    private static final Logger logger = LoggerFactory.getLogger(EmailSender.class);
    private static final String EMAIL_SERVER = "smtp.gmail.com";
    private static final String OFFICE_EMAIL = "office@kohang.com.hk";
    private static final String TESTING_KEY_WORD = "<測試>";
    protected static final String HTML_TEMPLATE = "<html>%s</html>";
    protected static final String HTML_TABLE_TEMPLATE = "<table>%s</table>";
    protected static final Long MAX_ATTACHMENT_SIZE = 26214400L; //25MB
    private final String username;
    private final String password;
    private final Properties prop;
    protected final String emailSubject;
    protected final String env;


    public EmailSender(String username, String password, String env, String emailSubject) {
        this.username = username.strip();
        this.password = password.strip();
        this.env = env;
        this.emailSubject = emailSubject;
        this.prop = new Properties();
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true");
        prop.put("mail.smtp.host", EMAIL_SERVER);
        prop.put("mail.smtp.port", "587");
    }

    public Message createMessage(String counter) throws MessagingException {
        final Session session = Session.getInstance(prop, null);
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(OFFICE_EMAIL));
        message.setSubject(this.getEmailSubject(counter));
        return message;
    }

    public void send(Message message) throws MessagingException {
        logger.info("Sending Email...");
        Transport.send(message, username, password);
    }

    private String getEmailSubject(String counter) {
        String emailSubject = String.format("%s-%s", this.emailSubject, counter);
        return env != null && env.equals("prod") ? emailSubject : emailSubject + " " + TESTING_KEY_WORD;
    }


}
