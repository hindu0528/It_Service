package com.ticketdesk.ticketservice.controller;

import com.ticketdesk.ticketservice.dto.request.AddCommentRequest;
import com.ticketdesk.ticketservice.dto.request.CreateTicketRequest;
import com.ticketdesk.ticketservice.dto.request.UpdateTicketStatusRequest;
import com.ticketdesk.ticketservice.dto.response.CommentResponse;
import com.ticketdesk.ticketservice.dto.response.DashboardSummaryResponse;
import com.ticketdesk.ticketservice.dto.response.TicketResponse;
import com.ticketdesk.ticketservice.enums.TicketCategory;
import com.ticketdesk.ticketservice.enums.TicketPriority;
import com.ticketdesk.ticketservice.enums.TicketStatus;
import com.ticketdesk.ticketservice.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Tag(name = "Ticket Controller", description = "Endpoints for ticket management, comments, filtering, and dashboard analytics")
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a new ticket", description = "Authenticated users can create tickets.")
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody CreateTicketRequest request,
                                                       Authentication authentication) {
        Long currentUserId = getUserIdFromAuthentication(authentication);
        TicketResponse response = ticketService.createTicket(request, currentUserId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List and filter tickets", description = "Filter by status, priority, category, assignment status with pagination.")
    public ResponseEntity<Page<TicketResponse>> getTickets(
            @RequestParam(name = "status", required = false) TicketStatus status,
            @RequestParam(name = "priority", required = false) TicketPriority priority,
            @RequestParam(name = "category", required = false) TicketCategory category,
            @RequestParam(name = "assignedStatus", required = false) String assignedStatus,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Authentication authentication) {
        Long currentUserId = getUserIdFromAuthentication(authentication);
        boolean isAdmin = isAdmin(authentication);
        boolean isAgent = isAgentOnly(authentication);
        Page<TicketResponse> response = ticketService.getTickets(
                status, priority, category, assignedStatus, page, size, currentUserId, isAdmin, isAgent
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get ticket details by ID", description = "Fetches a specific ticket by ID.")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable("id") Long id,
                                                         Authentication authentication) {
        Long currentUserId = getUserIdFromAuthentication(authentication);
        boolean isAgentOrAdmin = isAgentOrAdmin(authentication);
        TicketResponse response = ticketService.getTicketById(id, currentUserId, isAgentOrAdmin);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Update ticket status", description = "Only AGENT or ADMIN can update status. State transition rules strictly enforced.")
    public ResponseEntity<TicketResponse> updateTicketStatus(@PathVariable("id") Long id,
                                                             @Valid @RequestBody UpdateTicketStatusRequest request,
                                                             Authentication authentication) {
        Long currentUserId = getUserIdFromAuthentication(authentication);
        boolean isAgentOrAdmin = isAgentOrAdmin(authentication);
        TicketResponse response = ticketService.updateTicketStatus(id, request, currentUserId, isAgentOrAdmin);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign ticket to agent", description = "Only ADMIN can assign tickets to specific agents.")
    public ResponseEntity<TicketResponse> assignTicket(@PathVariable("id") Long id,
                                                        @RequestParam(name = "agentId", required = false) Long agentId,
                                                        Authentication authentication) {
        Long currentUserId = getUserIdFromAuthentication(authentication);
        boolean isAgentOrAdmin = isAgentOrAdmin(authentication);
        TicketResponse response = ticketService.assignTicket(id, agentId, currentUserId, isAgentOrAdmin);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add a comment to a ticket", description = "Adds a threaded note to the specified ticket.")
    public ResponseEntity<CommentResponse> addComment(@PathVariable("id") Long ticketId,
                                                      @Valid @RequestBody AddCommentRequest request,
                                                      Authentication authentication) {
        Long currentUserId = getUserIdFromAuthentication(authentication);
        boolean isAgentOrAdmin = isAgentOrAdmin(authentication);
        CommentResponse response = ticketService.addComment(ticketId, request, currentUserId, isAgentOrAdmin);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/comments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get comments for a ticket", description = "Returns all threaded comments for a ticket.")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable("id") Long ticketId,
                                                             Authentication authentication) {
        Long currentUserId = getUserIdFromAuthentication(authentication);
        boolean isAgentOrAdmin = isAgentOrAdmin(authentication);
        List<CommentResponse> response = ticketService.getCommentsByTicketId(ticketId, currentUserId, isAgentOrAdmin);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Get dashboard analytics", description = "Returns ticket counts grouped by status and priority.")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary() {
        DashboardSummaryResponse response = ticketService.getDashboardSummary();
        return ResponseEntity.ok(response);
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        return null;
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));
    }

    private boolean isAgentOnly(Authentication authentication) {
        if (authentication == null) return false;
        boolean hasAgent = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_AGENT"));
        return hasAgent && !isAdmin(authentication);
    }

    private boolean isAgentOrAdmin(Authentication authentication) {
        return isAdmin(authentication) || isAgentOnly(authentication);
    }
}
