package com.ticketdesk.notificationservice.event;

import java.time.LocalDateTime;

public record TicketCreatedEvent(
        String eventId,
        String eventType,
        Long ticketId,
        String title,
        Long createdBy,
        String priority,
        String category,
        LocalDateTime timestamp
) {}
