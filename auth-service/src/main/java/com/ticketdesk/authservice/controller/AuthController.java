package com.ticketdesk.authservice.controller;

import com.ticketdesk.authservice.dto.request.LoginRequest;
import com.ticketdesk.authservice.dto.request.RegisterRequest;
import com.ticketdesk.authservice.dto.response.AuthResponse;
import com.ticketdesk.authservice.dto.response.UserResponse;
import com.ticketdesk.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication Controller", description = "Endpoints for user registration, login, profile lookup, and agent list")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Public registration endpoint. ADMIN role cannot be registered.")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.registerUser(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Returns JWT bearer token upon successful credentials validation.")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.loginUser(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user by ID", description = "Internal service lookup endpoint used by downstream microservices.")
    public ResponseEntity<UserResponse> getUserById(@PathVariable("id") Long id) {
        UserResponse response = authService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/agents")
    @Operation(summary = "Get all agents", description = "Returns list of users with ROLE_AGENT for admin assignment lookup.")
    public ResponseEntity<List<UserResponse>> getAgents() {
        List<UserResponse> agents = authService.getAgents();
        return ResponseEntity.ok(agents);
    }
}
