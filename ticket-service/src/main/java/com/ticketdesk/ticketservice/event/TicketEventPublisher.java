package com.ticketdesk.ticketservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public static final String TOPIC_TICKET_CREATED = "ticket.created";
    public static final String TOPIC_TICKET_STATUS_CHANGED = "ticket.status-changed";
    public static final String TOPIC_TICKET_COMMENT_ADDED = "ticket.comment-added";

    public void publishTicketCreated(TicketCreatedEvent event) {
        log.info("Publishing TicketCreatedEvent for Ticket ID: {} to topic {}", event.ticketId(), TOPIC_TICKET_CREATED);
        try {
            kafkaTemplate.send(TOPIC_TICKET_CREATED, String.valueOf(event.ticketId()), event);
        } catch (Exception e) {
            log.error("Failed to publish TicketCreatedEvent to Kafka: {}", e.getMessage(), e);
        }
    }

    public void publishTicketStatusChanged(TicketStatusChangedEvent event) {
        log.info("Publishing TicketStatusChangedEvent for Ticket ID: {} ({}) to topic {}", event.ticketId(), event.newStatus(), TOPIC_TICKET_STATUS_CHANGED);
        try {
            kafkaTemplate.send(TOPIC_TICKET_STATUS_CHANGED, String.valueOf(event.ticketId()), event);
        } catch (Exception e) {
            log.error("Failed to publish TicketStatusChangedEvent to Kafka: {}", e.getMessage(), e);
        }
    }

    public void publishTicketCommentAdded(TicketCommentAddedEvent event) {
        log.info("Publishing TicketCommentAddedEvent for Comment ID: {} on Ticket ID: {} to topic {}", event.commentId(), event.ticketId(), TOPIC_TICKET_COMMENT_ADDED);
        try {
            kafkaTemplate.send(TOPIC_TICKET_COMMENT_ADDED, String.valueOf(event.ticketId()), event);
        } catch (Exception e) {
            log.error("Failed to publish TicketCommentAddedEvent to Kafka: {}", e.getMessage(), e);
        }
    }
}
