package com.ticketdesk.attachmentservice.mapper;

import com.ticketdesk.attachmentservice.dto.response.AttachmentResponse;
import com.ticketdesk.attachmentservice.entity.Attachment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AttachmentMapper {

    AttachmentResponse attachmentToAttachmentResponse(Attachment attachment);
}
