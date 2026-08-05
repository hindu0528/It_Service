package com.ticketdesk.attachmentservice.client;

import com.ticketdesk.attachmentservice.dto.response.TicketDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TicketServiceClientFallback implements TicketServiceClient {

    @Override
    public TicketDto getTicketById(Long id) {
        log.warn("Ticket Service Feign client circuit open/fallback triggered for Ticket ID: {}. Returning queued fallback TicketDto.", id);
        return TicketDto.builder()
                .id(id)
                .title("Queued Ticket (Service Degraded)")
                .status("UNKNOWN")
                .build();
    }
}
