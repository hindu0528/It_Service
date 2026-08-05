package com.ticketdesk.authservice.mapper;

import com.ticketdesk.authservice.dto.request.RegisterRequest;
import com.ticketdesk.authservice.dto.response.UserResponse;
import com.ticketdesk.authservice.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User registerRequestToUser(RegisterRequest request);

    UserResponse userToUserResponse(User user);
}
