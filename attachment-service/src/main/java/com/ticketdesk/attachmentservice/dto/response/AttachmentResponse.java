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
public class AttachmentResponse {

    private Long id;
    private Long ticketId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String presignedUrl;
    private AttachmentStatus status;
    private LocalDateTime createdAt;
}
