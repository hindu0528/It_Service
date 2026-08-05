package com.ticketdesk.attachmentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketDto {

    private Long id;
    private String title;
    private String description;
    private String category;
    private String priority;
    private String status;
    private Long createdBy;
    private Long assignedTo;
    private LocalDateTime createdAt;
}
