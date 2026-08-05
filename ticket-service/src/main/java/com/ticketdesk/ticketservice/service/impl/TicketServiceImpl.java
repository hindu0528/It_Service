package com.ticketdesk.ticketservice.service.impl;

import com.ticketdesk.ticketservice.client.AuthServiceClient;
import com.ticketdesk.ticketservice.dto.request.AddCommentRequest;
import com.ticketdesk.ticketservice.dto.request.CreateTicketRequest;
import com.ticketdesk.ticketservice.dto.request.UpdateTicketStatusRequest;
import com.ticketdesk.ticketservice.dto.response.*;
import com.ticketdesk.ticketservice.entity.Comment;
import com.ticketdesk.ticketservice.entity.Ticket;
import com.ticketdesk.ticketservice.enums.*;
import com.ticketdesk.ticketservice.event.*;
import com.ticketdesk.ticketservice.exception.InvalidStatusTransitionException;
import com.ticketdesk.ticketservice.exception.TicketNotFoundException;
import com.ticketdesk.ticketservice.exception.UnauthorizedAccessException;
import com.ticketdesk.ticketservice.mapper.CommentMapper;
import com.ticketdesk.ticketservice.mapper.TicketMapper;
import com.ticketdesk.ticketservice.repository.CommentRepository;
import com.ticketdesk.ticketservice.repository.TicketRepository;
import com.ticketdesk.ticketservice.service.TicketService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final AuthServiceClient authServiceClient;
    private final TicketMapper ticketMapper;
    private final CommentMapper commentMapper;
    private final TicketEventPublisher eventPublisher;

    @Override
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request, Long currentUserId) {
        log.info("Creating new ticket for user ID: {}, title: {}", currentUserId, request.getTitle());

        // Validate createdBy user exists via Feign
        UserDto creator = authServiceClient.getUserById(currentUserId);

        // Validate assignedTo user exists if specified
        UserDto assignee = null;
        if (request.getAssignedTo() != null) {
            assignee = authServiceClient.getUserById(request.getAssignedTo());
        }

        // Calculate individual user sequence number (starts at 1 for each user)
        long existingUserTickets = ticketRepository.countByCreatedBy(currentUserId);
        int userSeq = (int) existingUserTickets + 1;

        // Generate unique formatted ticket reference code (e.g. TKT-9842-01)
        int randomCode = 1000 + new Random().nextInt(9000);
        String ticketNum = String.format("TKT-%d-%02d", randomCode, userSeq);

        Ticket ticket = ticketMapper.createTicketRequestToTicket(request);
        ticket.setCreatedBy(currentUserId);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setTicketNumber(ticketNum);
        ticket.setUserSequence(userSeq);

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket created successfully with ID: {}, Ticket Number: {}", savedTicket.getId(), savedTicket.getTicketNumber());

        // Publish Kafka event
        TicketCreatedEvent event = new TicketCreatedEvent(
                UUID.randomUUID().toString(),
                EventType.TICKET_CREATED,
                savedTicket.getId(),
                savedTicket.getTitle(),
                savedTicket.getCreatedBy(),
                savedTicket.getPriority(),
                savedTicket.getCategory(),
                LocalDateTime.now()
        );
        eventPublisher.publishTicketCreated(event);

        return enrichTicketResponse(savedTicket, creator, assignee);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponse> getTickets(TicketStatus status, TicketPriority priority, TicketCategory category,
                                           String assignedStatus, int page, int size, Long currentUserId,
                                           boolean isAdmin, boolean isAgent) {
        log.info("Fetching tickets page {} size {}, filters: status={}, priority={}, category={}, assignedStatus={}, userId={}, isAdmin={}, isAgent={}",
                page, size, status, priority, category, assignedStatus, currentUserId, isAdmin, isAgent);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Ticket> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            // Role-Based Ticket Visibility & Ownership Policy
            if (isAdmin) {
                // Admin sees ALL tickets. Optional Assignment Filter: UNASSIGNED vs ASSIGNED
                if ("UNASSIGNED".equalsIgnoreCase(assignedStatus)) {
                    predicates.add(cb.isNull(root.get("assignedTo")));
                } else if ("ASSIGNED".equalsIgnoreCase(assignedStatus)) {
                    predicates.add(cb.isNotNull(root.get("assignedTo")));
                }
            } else if (isAgent) {
                // Agent ONLY sees tickets assigned directly to them
                if (currentUserId != null) {
                    predicates.add(cb.equal(root.get("assignedTo"), currentUserId));
                }
            } else {
                // Standard USER ONLY sees tickets created by them
                if (currentUserId != null) {
                    predicates.add(cb.equal(root.get("createdBy"), currentUserId));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Ticket> ticketPage = ticketRepository.findAll(spec, pageable);
        return ticketPage.map(this::mapAndEnrichTicket);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long id, Long currentUserId, boolean isAgentOrAdmin) {
        log.info("Fetching ticket by ID: {}", id);
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        // Enforce ownership rule for standard USER
        if (!isAgentOrAdmin && !ticket.getCreatedBy().equals(currentUserId) && !Objects.equals(ticket.getAssignedTo(), currentUserId)) {
            throw new UnauthorizedAccessException("You are not authorized to view ticket ID: " + id);
        }

        return mapAndEnrichTicket(ticket);
    }

    @Override
    @Transactional
    public TicketResponse updateTicketStatus(Long id, UpdateTicketStatusRequest request, Long currentUserId, boolean isAgentOrAdmin) {
        log.info("Updating ticket status for ID: {} to {}", id, request.getStatus());

        // Role Authorization: Only AGENT or ADMIN can update ticket status
        if (!isAgentOrAdmin) {
            throw new UnauthorizedAccessException("Only AGENT or ADMIN roles are authorized to change ticket status.");
        }

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        TicketStatus currentStatus = ticket.getStatus();
        TicketStatus newStatus = request.getStatus();

        // Enforce strict sequential status transition state machine rules
        validateStatusTransition(currentStatus, newStatus);

        ticket.setStatus(newStatus);
        Ticket updatedTicket = ticketRepository.save(ticket);
        log.info("Ticket ID: {} status updated from {} to {}", id, currentStatus, newStatus);

        // Publish Kafka Event
        TicketStatusChangedEvent event = new TicketStatusChangedEvent(
                UUID.randomUUID().toString(),
                EventType.TICKET_STATUS_CHANGED,
                updatedTicket.getId(),
                currentStatus,
                newStatus,
                currentUserId,
                LocalDateTime.now()
        );
        eventPublisher.publishTicketStatusChanged(event);

        return mapAndEnrichTicket(updatedTicket);
    }

    @Override
    @Transactional
    public TicketResponse assignTicket(Long id, Long targetAgentId, Long currentUserId, boolean isAgentOrAdmin) {
        log.info("Assigning ticket ID: {} to agent ID: {} by current user: {}", id, targetAgentId, currentUserId);

        if (!isAgentOrAdmin) {
            throw new UnauthorizedAccessException("Only AGENT or ADMIN roles are authorized to assign tickets.");
        }

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        Long assigneeId = (targetAgentId != null) ? targetAgentId : currentUserId;

        // Verify target agent exists via Feign
        UserDto assignee = authServiceClient.getUserById(assigneeId);

        ticket.setAssignedTo(assigneeId);
        Ticket updated = ticketRepository.save(ticket);
        log.info("Ticket ID: {} assigned to agent: {} (ID: {})", id, assignee.getUsername(), assigneeId);

        return mapAndEnrichTicket(updated);
    }

    @Override
    @Transactional
    public CommentResponse addComment(Long ticketId, AddCommentRequest request, Long currentUserId, boolean isAgentOrAdmin) {
        log.info("Adding comment to ticket ID: {} by user ID: {}", ticketId, currentUserId);

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        // Ownership rule: USER can only comment on tickets created by or assigned to them
        if (!isAgentOrAdmin && !ticket.getCreatedBy().equals(currentUserId) && !Objects.equals(ticket.getAssignedTo(), currentUserId)) {
            throw new UnauthorizedAccessException("You are not authorized to comment on ticket ID: " + ticketId);
        }

        UserDto author = authServiceClient.getUserById(currentUserId);

        Comment comment = Comment.builder()
                .ticketId(ticketId)
                .authorId(currentUserId)
                .text(request.getText())
                .build();

        Comment savedComment = commentRepository.save(comment);
        log.info("Comment added successfully with ID: {}", savedComment.getId());

        // Publish Kafka Event
        String snippet = request.getText().length() > 50 ? request.getText().substring(0, 50) + "..." : request.getText();
        TicketCommentAddedEvent event = new TicketCommentAddedEvent(
                UUID.randomUUID().toString(),
                EventType.TICKET_COMMENT_ADDED,
                savedComment.getId(),
                ticketId,
                currentUserId,
                snippet,
                LocalDateTime.now()
        );
        eventPublisher.publishTicketCommentAdded(event);

        CommentResponse response = commentMapper.commentToCommentResponse(savedComment);
        response.setAuthorUsername(author.getUsername());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByTicketId(Long ticketId, Long currentUserId, boolean isAgentOrAdmin) {
        log.info("Fetching comments for ticket ID: {}", ticketId);
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        if (!isAgentOrAdmin && !ticket.getCreatedBy().equals(currentUserId) && !Objects.equals(ticket.getAssignedTo(), currentUserId)) {
            throw new UnauthorizedAccessException("You are not authorized to view comments for ticket ID: " + ticketId);
        }

        List<Comment> comments = commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
        return comments.stream().map(comment -> {
            CommentResponse response = commentMapper.commentToCommentResponse(comment);
            try {
                UserDto author = authServiceClient.getUserById(comment.getAuthorId());
                response.setAuthorUsername(author.getUsername());
            } catch (Exception e) {
                response.setAuthorUsername("User " + comment.getAuthorId());
            }
            return response;
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary() {
        log.info("Generating dashboard counts summary by status and priority");

        long total = ticketRepository.count();

        Map<TicketStatus, Long> countsByStatus = new EnumMap<>(TicketStatus.class);
        for (TicketStatus s : TicketStatus.values()) {
            countsByStatus.put(s, ticketRepository.countByStatus(s));
        }

        Map<TicketPriority, Long> countsByPriority = new EnumMap<>(TicketPriority.class);
        for (TicketPriority p : TicketPriority.values()) {
            countsByPriority.put(p, ticketRepository.countByPriority(p));
        }

        return DashboardSummaryResponse.builder()
                .totalTickets(total)
                .countsByStatus(countsByStatus)
                .countsByPriority(countsByPriority)
                .build();
    }

    private void validateStatusTransition(TicketStatus current, TicketStatus target) {
        if (current == target) {
            return; // Same status, no-op
        }

        boolean valid = switch (current) {
            case OPEN -> target == TicketStatus.IN_PROGRESS;
            case IN_PROGRESS -> target == TicketStatus.RESOLVED || target == TicketStatus.OPEN;
            case RESOLVED -> target == TicketStatus.CLOSED || target == TicketStatus.IN_PROGRESS;
            case CLOSED -> target == TicketStatus.IN_PROGRESS; // Re-open
        };

        if (!valid) {
            String details = String.format(
                "Invalid status transition from '%s' to '%s'. Tickets must follow the strict sequential lifecycle order: OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED.",
                current, target
            );
            throw new InvalidStatusTransitionException(details);
        }
    }

    private TicketResponse mapAndEnrichTicket(Ticket ticket) {
        UserDto creator = null;
        try {
            creator = authServiceClient.getUserById(ticket.getCreatedBy());
        } catch (Exception e) {
            log.warn("Could not fetch creator details for user ID: {}", ticket.getCreatedBy());
        }

        UserDto assignee = null;
        if (ticket.getAssignedTo() != null) {
            try {
                assignee = authServiceClient.getUserById(ticket.getAssignedTo());
            } catch (Exception e) {
                log.warn("Could not fetch assignee details for user ID: {}", ticket.getAssignedTo());
            }
        }

        return enrichTicketResponse(ticket, creator, assignee);
    }

    private TicketResponse enrichTicketResponse(Ticket ticket, UserDto creator, UserDto assignee) {
        TicketResponse response = ticketMapper.ticketToTicketResponse(ticket);
        if (creator != null) {
            response.setCreatedByUsername(creator.getUsername());
        }
        if (assignee != null) {
            response.setAssignedToUsername(assignee.getUsername());
        }

        // Populate fallback ticketNumber and userSequence for legacy database records
        if (response.getTicketNumber() == null) {
            response.setTicketNumber("TKT-" + (1000 + ticket.getId()));
            response.setUserSequence(ticket.getId().intValue());
        }

        return response;
    }
}
