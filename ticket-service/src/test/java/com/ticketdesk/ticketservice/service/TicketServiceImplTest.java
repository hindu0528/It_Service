package com.ticketdesk.ticketservice.service;

import com.ticketdesk.ticketservice.client.AuthServiceClient;
import com.ticketdesk.ticketservice.dto.request.AddCommentRequest;
import com.ticketdesk.ticketservice.dto.request.CreateTicketRequest;
import com.ticketdesk.ticketservice.dto.request.UpdateTicketStatusRequest;
import com.ticketdesk.ticketservice.dto.response.CommentResponse;
import com.ticketdesk.ticketservice.dto.response.DashboardSummaryResponse;
import com.ticketdesk.ticketservice.dto.response.TicketResponse;
import com.ticketdesk.ticketservice.dto.response.UserDto;
import com.ticketdesk.ticketservice.entity.Comment;
import com.ticketdesk.ticketservice.entity.Ticket;
import com.ticketdesk.ticketservice.enums.TicketCategory;
import com.ticketdesk.ticketservice.enums.TicketPriority;
import com.ticketdesk.ticketservice.enums.TicketStatus;
import com.ticketdesk.ticketservice.event.TicketEventPublisher;
import com.ticketdesk.ticketservice.exception.InvalidStatusTransitionException;
import com.ticketdesk.ticketservice.exception.TicketNotFoundException;
import com.ticketdesk.ticketservice.exception.UnauthorizedAccessException;
import com.ticketdesk.ticketservice.mapper.CommentMapper;
import com.ticketdesk.ticketservice.mapper.TicketMapper;
import com.ticketdesk.ticketservice.repository.CommentRepository;
import com.ticketdesk.ticketservice.repository.TicketRepository;
import com.ticketdesk.ticketservice.service.impl.TicketServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private TicketEventPublisher eventPublisher;

    @InjectMocks
    private TicketServiceImpl ticketService;

    private UserDto mockCreator;
    private UserDto mockAgent;
    private Ticket mockTicket;
    private TicketResponse mockTicketResponse;

    @BeforeEach
    void setUp() {
        mockCreator = new UserDto(5L, "user2", "user2@test.com", Set.of("ROLE_USER"));
        mockAgent = new UserDto(2L, "agent", "agent@test.com", Set.of("ROLE_AGENT"));

        mockTicket = Ticket.builder()
                .id(1L)
                .ticketNumber("TKT-5491-01")
                .userSequence(1)
                .title("Software Crash")
                .description("App freezes on start")
                .category(TicketCategory.SOFTWARE)
                .priority(TicketPriority.HIGH)
                .status(TicketStatus.OPEN)
                .createdBy(5L)
                .assignedTo(null)
                .createdAt(LocalDateTime.now())
                .build();

        mockTicketResponse = TicketResponse.builder()
                .id(1L)
                .ticketNumber("TKT-5491-01")
                .userSequence(1)
                .title("Software Crash")
                .description("App freezes on start")
                .category(TicketCategory.SOFTWARE)
                .priority(TicketPriority.HIGH)
                .status(TicketStatus.OPEN)
                .createdBy(5L)
                .createdByUsername("user2")
                .assignedTo(null)
                .build();
    }

    @Test
    @DisplayName("createTicket: Should create ticket with per-user sequence and publish Kafka event")
    void createTicket_Success() {
        CreateTicketRequest request = new CreateTicketRequest("Software Crash", "App freezes on start", TicketCategory.SOFTWARE, TicketPriority.HIGH, null);

        given(authServiceClient.getUserById(5L)).willReturn(mockCreator);
        given(ticketRepository.countByCreatedBy(5L)).willReturn(0L);
        given(ticketMapper.createTicketRequestToTicket(any(CreateTicketRequest.class))).willReturn(mockTicket);
        given(ticketRepository.save(any(Ticket.class))).willReturn(mockTicket);
        given(ticketMapper.ticketToTicketResponse(mockTicket)).willReturn(mockTicketResponse);

        TicketResponse response = ticketService.createTicket(request, 5L);

        assertThat(response).isNotNull();
        assertThat(response.getTicketNumber()).isEqualTo("TKT-5491-01");
        assertThat(response.getUserSequence()).isEqualTo(1);
        assertThat(response.getCreatedByUsername()).isEqualTo("user2");
        verify(eventPublisher).publishTicketCreated(any());
    }

    @Test
    @DisplayName("getTicketById: Should return ticket details when user is owner")
    void getTicketById_Success() {
        given(ticketRepository.findById(1L)).willReturn(Optional.of(mockTicket));
        given(authServiceClient.getUserById(5L)).willReturn(mockCreator);
        given(ticketMapper.ticketToTicketResponse(mockTicket)).willReturn(mockTicketResponse);

        TicketResponse response = ticketService.getTicketById(1L, 5L, false);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Software Crash");
    }

    @Test
    @DisplayName("getTicketById: Should throw UnauthorizedAccessException when non-agent user views foreign ticket")
    void getTicketById_Unauthorized() {
        given(ticketRepository.findById(1L)).willReturn(Optional.of(mockTicket));

        assertThatThrownBy(() -> ticketService.getTicketById(1L, 99L, false))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("You are not authorized to view ticket ID: 1");
    }

    @Test
    @DisplayName("getTicketById: Should throw TicketNotFoundException when ticket ID does not exist")
    void getTicketById_NotFound() {
        given(ticketRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getTicketById(999L, 5L, true))
                .isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    @DisplayName("updateTicketStatus: Should update status OPEN -> IN_PROGRESS sequentially")
    void updateTicketStatus_ValidTransition() {
        UpdateTicketStatusRequest request = new UpdateTicketStatusRequest(TicketStatus.IN_PROGRESS);

        given(ticketRepository.findById(1L)).willReturn(Optional.of(mockTicket));
        given(ticketRepository.save(any(Ticket.class))).willReturn(mockTicket);
        given(authServiceClient.getUserById(5L)).willReturn(mockCreator);
        given(ticketMapper.ticketToTicketResponse(any())).willReturn(mockTicketResponse);

        TicketResponse response = ticketService.updateTicketStatus(1L, request, 2L, true);

        assertThat(response).isNotNull();
        verify(eventPublisher).publishTicketStatusChanged(any());
    }

    @Test
    @DisplayName("updateTicketStatus: Should throw InvalidStatusTransitionException on direct OPEN -> CLOSED jump")
    void updateTicketStatus_InvalidTransition() {
        UpdateTicketStatusRequest request = new UpdateTicketStatusRequest(TicketStatus.CLOSED);

        given(ticketRepository.findById(1L)).willReturn(Optional.of(mockTicket));

        assertThatThrownBy(() -> ticketService.updateTicketStatus(1L, request, 2L, true))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("Invalid status transition from 'OPEN' to 'CLOSED'");
    }

    @Test
    @DisplayName("assignTicket: Should assign ticket to specified agent ID")
    void assignTicket_Success() {
        given(ticketRepository.findById(1L)).willReturn(Optional.of(mockTicket));
        given(authServiceClient.getUserById(2L)).willReturn(mockAgent);
        given(authServiceClient.getUserById(5L)).willReturn(mockCreator);
        given(ticketRepository.save(any(Ticket.class))).willReturn(mockTicket);
        given(ticketMapper.ticketToTicketResponse(any())).willReturn(mockTicketResponse);

        TicketResponse response = ticketService.assignTicket(1L, 2L, 1L, true);

        assertThat(response).isNotNull();
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    @DisplayName("addComment: Should save comment and publish Kafka event")
    void addComment_Success() {
        AddCommentRequest request = new AddCommentRequest("Checking logs now");
        Comment comment = Comment.builder().id(10L).ticketId(1L).authorId(2L).text("Checking logs now").build();
        CommentResponse commentResponse = CommentResponse.builder().id(10L).ticketId(1L).text("Checking logs now").build();

        given(ticketRepository.findById(1L)).willReturn(Optional.of(mockTicket));
        given(authServiceClient.getUserById(2L)).willReturn(mockAgent);
        given(commentRepository.save(any(Comment.class))).willReturn(comment);
        given(commentMapper.commentToCommentResponse(comment)).willReturn(commentResponse);

        CommentResponse response = ticketService.addComment(1L, request, 2L, true);

        assertThat(response).isNotNull();
        assertThat(response.getAuthorUsername()).isEqualTo("agent");
        verify(eventPublisher).publishTicketCommentAdded(any());
    }

    @Test
    @DisplayName("getDashboardSummary: Should return aggregated metrics by status and priority")
    void getDashboardSummary_Success() {
        given(ticketRepository.count()).willReturn(10L);
        given(ticketRepository.countByStatus(any(TicketStatus.class))).willReturn(2L);
        given(ticketRepository.countByPriority(any(TicketPriority.class))).willReturn(2L);

        DashboardSummaryResponse summary = ticketService.getDashboardSummary();

        assertThat(summary).isNotNull();
        assertThat(summary.getTotalTickets()).isEqualTo(10L);
        assertThat(summary.getCountsByStatus()).hasSize(TicketStatus.values().length);
        assertThat(summary.getCountsByPriority()).hasSize(TicketPriority.values().length);
    }
}
