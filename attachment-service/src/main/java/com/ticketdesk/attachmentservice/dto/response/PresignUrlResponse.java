package com.ticketdesk.attachmentservice.dto.response;

import com.ticketdesk.attachmentservice.enums.AttachmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresignUrlResponse {

    private Long attachmentId;
    private String fileName;
    private String presignedUrl;
    private String uploadUrl;
    private int expiresInSeconds;
    private AttachmentStatus status;
    private LocalDateTime createdAt;
}
