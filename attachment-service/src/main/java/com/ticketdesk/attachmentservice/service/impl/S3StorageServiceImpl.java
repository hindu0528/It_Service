package com.ticketdesk.attachmentservice.service.impl;

import com.ticketdesk.attachmentservice.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@Primary
public class S3StorageServiceImpl implements StorageService {

    @Value("${S3_BUCKET_NAME:${aws.s3.bucket-name:}}")
    private String bucketName;

    @Value("${AWS_REGION:${aws.s3.region:ap-south-2}}")
    private String awsRegion;

    private static final int EXPIRATION_SECONDS = 900; // 15 minutes

    @Override
    public String generatePresignedUploadUrl(String fileName, String fileType) {
        String fileKey = "uploads/" + UUID.randomUUID() + "-" + fileName;

        if (bucketName == null || bucketName.isBlank()) {
            log.warn("S3 bucket name is not configured. Returning fallback presigned URL.");
            return String.format("https://ticketdesk-attachments-bucket.s3.%s.amazonaws.com/%s", awsRegion, fileKey);
        }

        try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(awsRegion))
                .build()) {

            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .contentType(fileType != null ? fileType : "application/octet-stream")
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(EXPIRATION_SECONDS))
                    .putObjectRequest(objectRequest)
                    .build();

            PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);
            String url = presignedRequest.url().toString();
            log.info("Generated real AWS S3 presigned upload URL for key '{}': {}", fileKey, url);
            return url;
        } catch (Exception e) {
            log.error("Failed to generate S3 presigned upload URL: {}", e.getMessage(), e);
            return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, awsRegion, fileKey);
        }
    }

    @Override
    public int getPresignedUrlExpirationSeconds() {
        return EXPIRATION_SECONDS;
    }

    @Override
    public void uploadToS3(String fileName, String fileType, byte[] bytes) {
        if (bucketName == null || bucketName.isBlank()) {
            log.warn("S3 bucket name is not configured. Skipping S3 upload.");
            return;
        }

        if (bytes == null || bytes.length == 0) {
            log.warn("File bytes are empty. Skipping S3 upload.");
            return;
        }

        String fileKey = "uploads/" + UUID.randomUUID() + "-" + fileName;
        log.info("Uploading file '{}' ({} bytes) directly to S3 bucket '{}' key '{}'...", fileName, bytes.length, bucketName, fileKey);

        try (S3Client s3Client = S3Client.builder()
                .region(Region.of(awsRegion))
                .build()) {

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .contentType(fileType != null ? fileType : "application/octet-stream")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));
            log.info("Successfully uploaded file '{}' to S3 bucket '{}' key '{}'. This will trigger the Lambda thumbnail generator!", fileName, bucketName, fileKey);
        } catch (Exception e) {
            log.error("Failed to upload file to S3 bucket '{}': {}", bucketName, e.getMessage(), e);
        }
    }
}
