package com.ticketdesk.ticketservice.exception;

import com.ticketdesk.ticketservice.enums.TicketStatus;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(TicketStatus currentStatus, TicketStatus newStatus) {
        super(String.format("Invalid ticket status transition from %s to %s", currentStatus, newStatus));
    }

    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
