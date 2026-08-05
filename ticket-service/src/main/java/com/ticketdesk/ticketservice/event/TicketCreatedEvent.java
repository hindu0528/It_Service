package com.ticketdesk.ticketservice.event;

import com.ticketdesk.ticketservice.enums.EventType;
import com.ticketdesk.ticketservice.enums.TicketCategory;
import com.ticketdesk.ticketservice.enums.TicketPriority;

import java.time.LocalDateTime;

public record TicketCreatedEvent(
        String eventId,
        EventType eventType,
        Long ticketId,
        String title,
        Long createdBy,
        TicketPriority priority,
        TicketCategory category,
        LocalDateTime timestamp
) {}
