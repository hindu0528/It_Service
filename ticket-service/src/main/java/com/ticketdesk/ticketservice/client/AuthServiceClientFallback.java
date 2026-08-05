package com.ticketdesk.ticketservice.client;

import com.ticketdesk.ticketservice.dto.response.UserDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
public class AuthServiceClientFallback implements AuthServiceClient {

    @Override
    public UserDto getUserById(Long id) {
        log.warn("Auth Service Feign client circuit open/fallback triggered for User ID: {}. Returning degraded UserDto fallback.", id);
        return UserDto.builder()
                .id(id)
                .username("Unknown User (Service Unavailable)")
                .email("unknown@ticketdesk.com")
                .roles(Set.of("ROLE_USER"))
                .build();
    }
}
