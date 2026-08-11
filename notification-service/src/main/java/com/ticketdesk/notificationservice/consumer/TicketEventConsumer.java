package com.ticketdesk.notificationservice.consumer;

import com.ticketdesk.notificationservice.event.TicketCommentAddedEvent;
import com.ticketdesk.notificationservice.event.TicketCreatedEvent;
import com.ticketdesk.notificationservice.event.TicketStatusChangedEvent;
import com.ticketdesk.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class TicketEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "ticket.created", groupId = "notification-group")
    public void consumeTicketCreated(TicketCreatedEvent event) {
        log.info("Kafka Consumer received TicketCreatedEvent for ticket ID: {}", event.ticketId());
        notificationService.handleTicketCreated(event);
    }

    @KafkaListener(topics = "ticket.status-changed", groupId = "notification-group")
    public void consumeTicketStatusChanged(TicketStatusChangedEvent event) {
        log.info("Kafka Consumer received TicketStatusChangedEvent for ticket ID: {}", event.ticketId());
        notificationService.handleTicketStatusChanged(event);
    }

    @KafkaListener(topics = "ticket.comment-added", groupId = "notification-group")
    public void consumeTicketCommentAdded(TicketCommentAddedEvent event) {
        log.info("Kafka Consumer received TicketCommentAddedEvent for comment ID: {} on ticket ID: {}",
                event.commentId(), event.ticketId());
        notificationService.handleTicketCommentAdded(event);
    }
}
