package com.ticketdesk.ticketservice.dto.response;

import com.ticketdesk.ticketservice.enums.TicketPriority;
import com.ticketdesk.ticketservice.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryResponse {

    private long totalTickets;
    private Map<TicketStatus, Long> countsByStatus;
    private Map<TicketPriority, Long> countsByPriority;
}
