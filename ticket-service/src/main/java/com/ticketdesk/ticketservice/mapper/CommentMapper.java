package com.ticketdesk.ticketservice.mapper;

import com.ticketdesk.ticketservice.dto.response.CommentResponse;
import com.ticketdesk.ticketservice.entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "authorUsername", ignore = true)
    CommentResponse commentToCommentResponse(Comment comment);
}
