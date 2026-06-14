package com.hometusk.notifications.email.sender;

public record EmailMessage(String recipientEmail, String subject, String bodyText, String bodyHtml) {}
