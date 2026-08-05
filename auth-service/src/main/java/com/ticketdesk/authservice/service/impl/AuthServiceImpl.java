package com.ticketdesk.authservice.service.impl;

import com.ticketdesk.authservice.dto.request.LoginRequest;
import com.ticketdesk.authservice.dto.request.RegisterRequest;
import com.ticketdesk.authservice.dto.response.AuthResponse;
import com.ticketdesk.authservice.dto.response.UserResponse;
import com.ticketdesk.authservice.entity.User;
import com.ticketdesk.authservice.enums.Role;
import com.ticketdesk.authservice.exception.AdminRegistrationNotAllowedException;
import com.ticketdesk.authservice.exception.InvalidCredentialsException;
import com.ticketdesk.authservice.exception.ResourceNotFoundException;
import com.ticketdesk.authservice.exception.UserAlreadyExistsException;
import com.ticketdesk.authservice.mapper.UserMapper;
import com.ticketdesk.authservice.repository.UserRepository;
import com.ticketdesk.authservice.service.AuthService;
import com.ticketdesk.authservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponse registerUser(RegisterRequest request) {
        log.info("Attempting user registration for username: {}, email: {}", request.getUsername(), request.getEmail());

        // Enforce strict security rule: ADMIN role cannot be self-registered
        if (request.getRole() == Role.ROLE_ADMIN) {
            throw new AdminRegistrationNotAllowedException("Admin role cannot be self-registered via public endpoint. Admin users must be provisioned internally.");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username '" + request.getUsername() + "' is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email '" + request.getEmail() + "' is already registered");
        }

        User user = userMapper.registerRequestToUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Set<Role> roles = new HashSet<>();
        if (request.getRole() == Role.ROLE_AGENT) {
            roles.add(Role.ROLE_AGENT);
            roles.add(Role.ROLE_USER);
        } else {
            roles.add(Role.ROLE_USER);
        }
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}, roles: {}", savedUser.getId(), savedUser.getRoles());

        return userMapper.userToUserResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse loginUser(LoginRequest request) {
        log.info("Attempting authentication for user: {}", request.getUsername());

        User user = userRepository.findByUsernameOrEmail(request.getUsername(), request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        String token = jwtUtil.generateToken(user);
        log.info("Authentication successful for user: {}", user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        log.info("Fetching user details for ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        return userMapper.userToUserResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAgents() {
        log.info("Fetching all agents for admin assignment lookup");
        List<User> agents = userRepository.findByRolesContaining(Role.ROLE_AGENT);
        return agents.stream().map(userMapper::userToUserResponse).toList();
    }
}
