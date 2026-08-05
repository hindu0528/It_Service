package com.ticketdesk.attachmentservice.exception;

public class AttachmentNotFoundException extends ResourceNotFoundException {
    public AttachmentNotFoundException(Long id) {
        super("Attachment not found with ID: " + id);
    }
}
