package com.ticketdesk.ticketservice.mapper;

import com.ticketdesk.ticketservice.dto.request.CreateTicketRequest;
import com.ticketdesk.ticketservice.dto.response.TicketResponse;
import com.ticketdesk.ticketservice.entity.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Ticket createTicketRequestToTicket(CreateTicketRequest request);

    @Mapping(target = "createdByUsername", ignore = true)
    @Mapping(target = "assignedToUsername", ignore = true)
    TicketResponse ticketToTicketResponse(Ticket ticket);
}
