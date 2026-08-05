package com.ticketdesk.attachmentservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresignUrlRequest {

    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "File type (MIME type) is required")
    private String fileType;

    @NotNull(message = "File size is required")
    @Min(value = 1, message = "File size must be greater than 0 bytes")
    private Long fileSize;

    private Long ticketId; // Optional ticket ID if presigning for an existing ticket
}
