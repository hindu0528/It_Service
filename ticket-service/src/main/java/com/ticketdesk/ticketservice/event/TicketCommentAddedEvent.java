package com.ticketdesk.ticketservice.event;

import com.ticketdesk.ticketservice.enums.EventType;

import java.time.LocalDateTime;

public record TicketCommentAddedEvent(
        String eventId,
        EventType eventType,
        Long commentId,
        Long ticketId,
        Long authorId,
        String textSnippet,
        LocalDateTime timestamp
) {}
