package com.ticketdesk.notificationservice.service.impl;

import com.ticketdesk.notificationservice.event.TicketCommentAddedEvent;
import com.ticketdesk.notificationservice.event.TicketCreatedEvent;
import com.ticketdesk.notificationservice.event.TicketStatusChangedEvent;
import com.ticketdesk.notificationservice.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    @Override
    public void handleTicketCreated(TicketCreatedEvent event) {
        log.info("==========================================================================");
        log.info("[NOTIFICATION DISPATCHED] Event: TICKET_CREATED | EventId: {}", event.eventId());
        log.info("Recipient (CreatedBy User ID): {}", event.createdBy());
        log.info("Ticket ID: {} | Title: '{}' | Priority: {} | Category: {}",
                event.ticketId(), event.title(), event.priority(), event.category());
        log.info("Message: Your ticket #{} has been successfully created.", event.ticketId());
        log.info("==========================================================================");
    }

    @Override
    public void handleTicketStatusChanged(TicketStatusChangedEvent event) {
        log.info("==========================================================================");
        log.info("[NOTIFICATION DISPATCHED] Event: TICKET_STATUS_CHANGED | EventId: {}", event.eventId());
        log.info("Updated By User ID: {}", event.updatedBy());
        log.info("Ticket ID: {} | Status Transition: {} -> {}", event.ticketId(), event.oldStatus(), event.newStatus());
        log.info("Message: Ticket #{} status changed from {} to {}.", event.ticketId(), event.oldStatus(), event.newStatus());
        log.info("==========================================================================");
    }

    @Override
    public void handleTicketCommentAdded(TicketCommentAddedEvent event) {
        log.info("==========================================================================");
        log.info("[NOTIFICATION DISPATCHED] Event: TICKET_COMMENT_ADDED | EventId: {}", event.eventId());
        log.info("Author User ID: {}", event.authorId());
        log.info("Ticket ID: {} | Comment ID: {}", event.ticketId(), event.commentId());
        log.info("Message: New comment added on Ticket #{}: '{}'", event.ticketId(), event.textSnippet());
        log.info("==========================================================================");
    }
}
