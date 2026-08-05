package com.ticketdesk.ticketservice.client;

import com.ticketdesk.ticketservice.dto.response.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service", fallback = AuthServiceClientFallback.class)
public interface AuthServiceClient {

    @GetMapping("/auth/users/{id}")
    UserDto getUserById(@PathVariable("id") Long id);
}
