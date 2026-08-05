package com.ticketdesk.attachmentservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmAttachmentRequest {

    @NotNull(message = "Attachment ID is required")
    private Long attachmentId;

    @NotNull(message = "Ticket ID is required")
    private Long ticketId;
}
