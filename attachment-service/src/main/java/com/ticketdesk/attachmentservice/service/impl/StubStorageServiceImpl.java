package com.ticketdesk.attachmentservice.service.impl;

import com.ticketdesk.attachmentservice.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class StubStorageServiceImpl implements StorageService {

    private static final String BUCKET_NAME = "ticketdesk-attachments-bucket";
    private static final String AWS_REGION = "us-east-1";
    private static final int EXPIRATION_SECONDS = 900; // 15 minutes

    @Override
    public String generatePresignedUploadUrl(String fileName, String fileType) {
        String fileKey = "uploads/" + UUID.randomUUID() + "-" + fileName;
        String mockSignature = UUID.randomUUID().toString().replace("-", "");
        
        // Generate valid mock preview link for dev environment
        String presignedUrl = String.format(
                "https://%s.s3.%s.amazonaws.com/%s?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIAIOSFODNN7EXAMPLE/20260803/%s/s3/aws4_request&X-Amz-Date=20260803T120000Z&X-Amz-Expires=%d&X-Amz-SignedHeaders=host&X-Amz-Signature=%s",
                BUCKET_NAME, AWS_REGION, fileKey, AWS_REGION, EXPIRATION_SECONDS, mockSignature
        );

        log.info("Generated presigned S3 URL for file '{}' (MIME: {}): {}", fileName, fileType, presignedUrl);
        return presignedUrl;
    }

    @Override
    public int getPresignedUrlExpirationSeconds() {
        return EXPIRATION_SECONDS;
    }

    @Override
    public void uploadToS3(String fileName, String fileType, byte[] bytes) {
        log.info("StubStorageServiceImpl: Simulated S3 upload for file '{}' ({} bytes)", fileName, bytes != null ? bytes.length : 0);
    }
}
