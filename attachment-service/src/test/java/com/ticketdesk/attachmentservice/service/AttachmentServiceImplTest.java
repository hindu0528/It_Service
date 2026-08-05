package com.ticketdesk.attachmentservice.service;

import com.ticketdesk.attachmentservice.client.TicketServiceClient;
import com.ticketdesk.attachmentservice.dto.request.ConfirmAttachmentRequest;
import com.ticketdesk.attachmentservice.dto.request.PresignUrlRequest;
import com.ticketdesk.attachmentservice.dto.response.AttachmentResponse;
import com.ticketdesk.attachmentservice.dto.response.PresignUrlResponse;
import com.ticketdesk.attachmentservice.dto.response.TicketDto;
import com.ticketdesk.attachmentservice.entity.Attachment;
import com.ticketdesk.attachmentservice.enums.AttachmentStatus;
import com.ticketdesk.attachmentservice.exception.AttachmentNotFoundException;
import com.ticketdesk.attachmentservice.mapper.AttachmentMapper;
import com.ticketdesk.attachmentservice.repository.AttachmentRepository;
import com.ticketdesk.attachmentservice.service.impl.AttachmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceImplTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private TicketServiceClient ticketServiceClient;

    @Mock
    private AttachmentMapper attachmentMapper;

    @InjectMocks
    private AttachmentServiceImpl attachmentService;

    private Attachment mockAttachment;
    private AttachmentResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockAttachment = Attachment.builder()
                .id(1L)
                .ticketId(10L)
                .fileName("error_screenshot.png")
                .fileType("image/png")
                .fileSize(102400L)
                .presignedUrl("https://s3.stub.url/error_screenshot.png")
                .status(AttachmentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        mockResponse = AttachmentResponse.builder()
                .id(1L)
                .ticketId(10L)
                .fileName("error_screenshot.png")
                .fileType("image/png")
                .fileSize(102400L)
                .presignedUrl("https://s3.stub.url/error_screenshot.png")
                .status(AttachmentStatus.LINKED)
                .build();
    }

    @Test
    @DisplayName("generatePresignedUrl: Should issue S3 upload presigned URL and save pending attachment")
    void generatePresignedUrl_Success() {
        PresignUrlRequest request = PresignUrlRequest.builder()
                .ticketId(10L)
                .fileName("error_screenshot.png")
                .fileType("image/png")
                .fileSize(102400L)
                .build();

        given(storageService.generatePresignedUploadUrl("error_screenshot.png", "image/png"))
                .willReturn("https://s3.stub.url/error_screenshot.png");
        given(storageService.getPresignedUrlExpirationSeconds()).willReturn(900);
        given(attachmentRepository.save(any(Attachment.class))).willReturn(mockAttachment);

        PresignUrlResponse response = attachmentService.generatePresignedUrl(request);

        assertThat(response).isNotNull();
        assertThat(response.getAttachmentId()).isEqualTo(1L);
        assertThat(response.getPresignedUrl()).isEqualTo("https://s3.stub.url/error_screenshot.png");
        verify(attachmentRepository).save(any(Attachment.class));
    }

    @Test
    @DisplayName("confirmAttachment: Should verify target ticket via Feign and mark status LINKED")
    void confirmAttachment_Success() {
        TicketDto ticketDto = TicketDto.builder().id(10L).title("Hardware issue").status("OPEN").build();

        given(attachmentRepository.findById(1L)).willReturn(Optional.of(mockAttachment));
        given(ticketServiceClient.getTicketById(10L)).willReturn(ticketDto);
        given(attachmentRepository.save(any(Attachment.class))).willReturn(mockAttachment);
        given(attachmentMapper.attachmentToAttachmentResponse(mockAttachment)).willReturn(mockResponse);

        AttachmentResponse response = attachmentService.confirmAttachment(10L, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(AttachmentStatus.LINKED);
        verify(attachmentRepository).save(any(Attachment.class));
    }

    @Test
    @DisplayName("confirmAttachment: Should throw AttachmentNotFoundException when attachment ID invalid")
    void confirmAttachment_NotFound() {
        given(attachmentRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> attachmentService.confirmAttachment(10L, 99L))
                .isInstanceOf(AttachmentNotFoundException.class);
    }

    @Test
    @DisplayName("getAttachmentsByTicketId: Should return all attachments linked to ticket ID")
    void getAttachmentsByTicketId_Success() {
        given(attachmentRepository.findByTicketId(10L)).willReturn(List.of(mockAttachment));
        given(attachmentMapper.attachmentToAttachmentResponse(mockAttachment)).willReturn(mockResponse);

        List<AttachmentResponse> list = attachmentService.getAttachmentsByTicketId(10L);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getFileName()).isEqualTo("error_screenshot.png");
    }
}
