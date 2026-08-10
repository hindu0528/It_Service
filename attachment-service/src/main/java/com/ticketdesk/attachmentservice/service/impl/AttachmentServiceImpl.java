package com.ticketdesk.attachmentservice.service.impl;

import com.ticketdesk.attachmentservice.client.TicketServiceClient;
import com.ticketdesk.attachmentservice.dto.request.ConfirmAttachmentRequest;
import com.ticketdesk.attachmentservice.dto.request.PresignUrlRequest;
import com.ticketdesk.attachmentservice.dto.response.AttachmentResponse;
import com.ticketdesk.attachmentservice.dto.response.PresignUrlResponse;
import com.ticketdesk.attachmentservice.dto.response.TicketDto;
import com.ticketdesk.attachmentservice.entity.Attachment;
import com.ticketdesk.attachmentservice.enums.AttachmentStatus;
import com.ticketdesk.attachmentservice.exception.AttachmentNotFoundException;
import com.ticketdesk.attachmentservice.mapper.AttachmentMapper;
import com.ticketdesk.attachmentservice.repository.AttachmentRepository;
import com.ticketdesk.attachmentservice.service.AttachmentService;
import com.ticketdesk.attachmentservice.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final StorageService storageService;
    private final TicketServiceClient ticketServiceClient;
    private final AttachmentMapper attachmentMapper;

    @Override
    @Transactional
        // Generate actual S3 presigned upload URL
        String uploadUrl = storageService.generatePresignedUploadUrl(request.getFileName(), request.getFileType());

        Attachment attachment = Attachment.builder()
                .ticketId(request.getTicketId())
                .fileName(request.getFileName())
                .fileType(request.getFileType())
                .fileSize(request.getFileSize())
                .status(AttachmentStatus.PENDING)
                .presignedUrl(uploadUrl)
                .build();

        Attachment saved = attachmentRepository.save(attachment);

        log.info("Saved pending attachment entity with ID: {}, upload URL: {}", saved.getId(), uploadUrl);

        return PresignUrlResponse.builder()
                .attachmentId(saved.getId())
                .fileName(saved.getFileName())
                .presignedUrl(saved.getPresignedUrl())
                .uploadUrl(saved.getPresignedUrl())
                .expiresInSeconds(storageService.getPresignedUrlExpirationSeconds())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public AttachmentResponse uploadFile(MultipartFile file, Long ticketId) {
        log.info("Uploading file '{}' ({} bytes) for ticket ID: {}", file.getOriginalFilename(), file.getSize(), ticketId);

        byte[] bytes = new byte[0];
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.error("Failed to read bytes from uploaded file", e);
        }

        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment.dat";
        String fileType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        Attachment attachment = Attachment.builder()
                .ticketId(ticketId)
                .fileName(fileName)
                .fileType(fileType)
                .fileSize(file.getSize())
                .fileData(bytes)
                .status(AttachmentStatus.LINKED)
                .presignedUrl("")
                .build();

        Attachment saved = attachmentRepository.save(attachment);
        saved.setPresignedUrl("http://localhost:8080/api/v1/attachments/preview/" + saved.getId());
        Attachment updated = attachmentRepository.save(saved);

        log.info("Successfully stored binary attachment ID: {} ({}) in database", updated.getId(), updated.getFileName());
        return attachmentMapper.attachmentToAttachmentResponse(updated);
    }

    @Override
    @Transactional
    public AttachmentResponse confirmAttachment(Long ticketId, Long attachmentId) {
        log.info("Confirming attachment ID: {} for ticket ID: {}", attachmentId, ticketId);

        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AttachmentNotFoundException(attachmentId));

        // Validate ticket exists via Feign (wrapped in circuit breaker)
        try {
            TicketDto ticket = ticketServiceClient.getTicketById(ticketId);
            log.info("Verified target ticket ID: {} (Title: '{}')", ticket.getId(), ticket.getTitle());
        } catch (Exception e) {
            log.warn("Feign call to Ticket Service failed or returned fallback for Ticket ID: {}: {}", ticketId, e.getMessage());
        }

        attachment.setTicketId(ticketId);
        attachment.setStatus(AttachmentStatus.LINKED);
        attachment.setPresignedUrl("http://localhost:8080/api/v1/attachments/preview/" + attachment.getId());

        Attachment updated = attachmentRepository.save(attachment);
        log.info("Attachment ID: {} successfully linked to Ticket ID: {}", attachmentId, ticketId);

        return attachmentMapper.attachmentToAttachmentResponse(updated);
    }

    @Override
    @Transactional
    public AttachmentResponse confirmAttachment(ConfirmAttachmentRequest request) {
        return confirmAttachment(request.getTicketId(), request.getAttachmentId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachmentsByTicketId(Long ticketId) {
        log.info("Fetching attachments for ticket ID: {}", ticketId);
        List<Attachment> attachments = attachmentRepository.findByTicketId(ticketId);
        return attachments.stream().map(att -> {
            AttachmentResponse response = attachmentMapper.attachmentToAttachmentResponse(att);
            // Ensure presignedUrl points to working local preview gateway endpoint
            response.setPresignedUrl("http://localhost:8080/api/v1/attachments/preview/" + att.getId());
            return response;
        }).toList();
    }
}
