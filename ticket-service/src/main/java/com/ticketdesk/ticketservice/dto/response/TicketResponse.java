package com.ticketdesk.ticketservice.dto.response;

import com.ticketdesk.ticketservice.enums.TicketCategory;
import com.ticketdesk.ticketservice.enums.TicketPriority;
import com.ticketdesk.ticketservice.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponse {

    private Long id;
    private String ticketNumber;
    private Integer userSequence;
    private String title;
    private String description;
    private TicketCategory category;
    private TicketPriority priority;
    private TicketStatus status;
    private Long createdBy;
    private String createdByUsername;
    private Long assignedTo;
    private String assignedToUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
