package com.ticketdesk.ticketservice.service;

import com.ticketdesk.ticketservice.dto.request.AddCommentRequest;
import com.ticketdesk.ticketservice.dto.request.CreateTicketRequest;
import com.ticketdesk.ticketservice.dto.request.UpdateTicketStatusRequest;
import com.ticketdesk.ticketservice.dto.response.CommentResponse;
import com.ticketdesk.ticketservice.dto.response.DashboardSummaryResponse;
import com.ticketdesk.ticketservice.dto.response.TicketResponse;
import com.ticketdesk.ticketservice.enums.TicketCategory;
import com.ticketdesk.ticketservice.enums.TicketPriority;
import com.ticketdesk.ticketservice.enums.TicketStatus;
import org.springframework.data.domain.Page;

import java.util.List;

public interface TicketService {

    TicketResponse createTicket(CreateTicketRequest request, Long currentUserId);

    Page<TicketResponse> getTickets(TicketStatus status, TicketPriority priority, TicketCategory category,
                                    String assignedStatus, int page, int size, Long currentUserId,
                                    boolean isAdmin, boolean isAgent);

    TicketResponse getTicketById(Long id, Long currentUserId, boolean isAgentOrAdmin);

    TicketResponse updateTicketStatus(Long id, UpdateTicketStatusRequest request, Long currentUserId, boolean isAgentOrAdmin);

    TicketResponse assignTicket(Long id, Long agentId, Long currentUserId, boolean isAgentOrAdmin);

    CommentResponse addComment(Long ticketId, AddCommentRequest request, Long currentUserId, boolean isAgentOrAdmin);

    List<CommentResponse> getCommentsByTicketId(Long ticketId, Long currentUserId, boolean isAgentOrAdmin);

    DashboardSummaryResponse getDashboardSummary();
}
