package com.ticketdesk.notificationservice.event;

import java.time.LocalDateTime;

public record TicketStatusChangedEvent(
        String eventId,
        String eventType,
        Long ticketId,
        String oldStatus,
        String newStatus,
        Long updatedBy,
        LocalDateTime timestamp
) {}
