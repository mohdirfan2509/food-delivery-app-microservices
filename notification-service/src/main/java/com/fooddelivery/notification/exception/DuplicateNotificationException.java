package com.fooddelivery.notification.exception;

public class DuplicateNotificationException extends RuntimeException {
    public DuplicateNotificationException(String message) {
        super(message);
    }
}
