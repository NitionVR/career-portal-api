package com.etalente.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
@Profile({"local", "dev", "maildev", "google-smtp"})
public class MailConfig {

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private Integer port;

    @Value("${spring.mail.username:}") // Default to empty string if not set
    private String username;

    @Value("${spring.mail.password:}") // Default to empty string if not set
    private String password;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);

        if (username != null && !username.isEmpty()) {
            mailSender.setUsername(username);
        }
        if (password != null && !password.isEmpty()) {
            mailSender.setPassword(password);
        }

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        // Connection pooling properties for better performance
        props.put("mail.smtp.connectiontimeout", "5000"); // 5 seconds
        props.put("mail.smtp.timeout", "5000"); // 5 seconds
        props.put("mail.smtp.writetimeout", "5000"); // 5 seconds

        return mailSender;
    }
}
