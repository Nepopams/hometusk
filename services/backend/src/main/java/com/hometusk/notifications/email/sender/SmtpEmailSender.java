package com.hometusk.notifications.email.sender;

import com.hometusk.notifications.email.config.EmailNotificationProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "hometusk.email", name = "sender", havingValue = "smtp")
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final EmailNotificationProperties properties;

    public SmtpEmailSender(JavaMailSender mailSender, EmailNotificationProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void send(EmailMessage message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.getFrom());
            helper.setTo(message.recipientEmail());
            helper.setSubject(message.subject());
            if (message.bodyHtml() == null || message.bodyHtml().isBlank()) {
                helper.setText(message.bodyText(), false);
            } else {
                helper.setText(message.bodyText(), message.bodyHtml());
            }
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new EmailSendException("SMTP message build failed", e);
        }
    }
}
