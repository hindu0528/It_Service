package com.ticketdesk.ticketservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {

    private Long id;
    private Long ticketId;
    private Long authorId;
    private String authorUsername;
    private String text;
    private LocalDateTime createdAt;
}
