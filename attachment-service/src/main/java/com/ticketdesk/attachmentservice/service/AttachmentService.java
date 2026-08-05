package com.ticketdesk.attachmentservice.service;

import com.ticketdesk.attachmentservice.dto.request.ConfirmAttachmentRequest;
import com.ticketdesk.attachmentservice.dto.request.PresignUrlRequest;
import com.ticketdesk.attachmentservice.dto.response.AttachmentResponse;
import com.ticketdesk.attachmentservice.dto.response.PresignUrlResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AttachmentService {

    PresignUrlResponse generatePresignedUrl(PresignUrlRequest request);

    AttachmentResponse uploadFile(MultipartFile file, Long ticketId);

    AttachmentResponse confirmAttachment(Long ticketId, Long attachmentId);

    AttachmentResponse confirmAttachment(ConfirmAttachmentRequest request);

    List<AttachmentResponse> getAttachmentsByTicketId(Long ticketId);
}
