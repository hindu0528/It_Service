package com.ticketdesk.attachmentservice.client;

import com.ticketdesk.attachmentservice.dto.response.TicketDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ticket-service", fallback = TicketServiceClientFallback.class)
public interface TicketServiceClient {

    @GetMapping("/tickets/{id}")
    TicketDto getTicketById(@PathVariable("id") Long id);
}
