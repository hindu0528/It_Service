package com.ticketdesk.notificationservice.service;

import com.ticketdesk.notificationservice.event.TicketCommentAddedEvent;
import com.ticketdesk.notificationservice.event.TicketCreatedEvent;
import com.ticketdesk.notificationservice.event.TicketStatusChangedEvent;

public interface NotificationService {

    void handleTicketCreated(TicketCreatedEvent event);

    void handleTicketStatusChanged(TicketStatusChangedEvent event);

    void handleTicketCommentAdded(TicketCommentAddedEvent event);
}
