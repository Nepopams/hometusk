package com.hometusk.notifications.email.sender;

public class EmailSendException extends RuntimeException {

    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
