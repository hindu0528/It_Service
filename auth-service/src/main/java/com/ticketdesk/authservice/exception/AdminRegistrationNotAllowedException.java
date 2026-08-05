package com.ticketdesk.authservice.exception;

public class AdminRegistrationNotAllowedException extends RuntimeException {
    public AdminRegistrationNotAllowedException(String message) {
        super(message);
    }
}
