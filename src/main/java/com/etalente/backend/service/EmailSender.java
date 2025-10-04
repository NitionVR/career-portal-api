package com.etalente.backend.service;

public interface EmailSender {
    void send(String to, String subject, String htmlBody);
}
