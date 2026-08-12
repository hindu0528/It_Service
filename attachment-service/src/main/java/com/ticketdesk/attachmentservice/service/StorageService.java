package com.ticketdesk.attachmentservice.service;

public interface StorageService {

    String generatePresignedUploadUrl(String fileName, String fileType);

    int getPresignedUrlExpirationSeconds();

    void uploadToS3(String fileName, String fileType, byte[] bytes);
}
