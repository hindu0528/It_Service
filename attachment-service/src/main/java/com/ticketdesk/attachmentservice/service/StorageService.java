package com.ticketdesk.attachmentservice.service;

public interface StorageService {

    String generatePresignedUploadUrl(String fileName, String fileType);

    int getPresignedUrlExpirationSeconds();
}
