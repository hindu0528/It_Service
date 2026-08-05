package com.ticketdesk.authservice.service;

import com.ticketdesk.authservice.dto.request.LoginRequest;
import com.ticketdesk.authservice.dto.request.RegisterRequest;
import com.ticketdesk.authservice.dto.response.AuthResponse;
import com.ticketdesk.authservice.dto.response.UserResponse;

import java.util.List;

public interface AuthService {

    UserResponse registerUser(RegisterRequest request);

    AuthResponse loginUser(LoginRequest request);

    UserResponse getUserById(Long userId);

    List<UserResponse> getAgents();
}
