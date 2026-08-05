package com.ticketdesk.notificationservice.event;

import java.time.LocalDateTime;

public record TicketCommentAddedEvent(
        String eventId,
        String eventType,
        Long commentId,
        Long ticketId,
        Long authorId,
        String textSnippet,
        LocalDateTime timestamp
) {}
