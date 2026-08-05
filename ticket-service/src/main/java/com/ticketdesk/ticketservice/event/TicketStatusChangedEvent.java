package com.ticketdesk.ticketservice.event;

import com.ticketdesk.ticketservice.enums.EventType;
import com.ticketdesk.ticketservice.enums.TicketStatus;

import java.time.LocalDateTime;

public record TicketStatusChangedEvent(
        String eventId,
        EventType eventType,
        Long ticketId,
        TicketStatus oldStatus,
        TicketStatus newStatus,
        Long updatedBy,
        LocalDateTime timestamp
) {}
