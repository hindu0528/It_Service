package com.ticketdesk.authservice.service;

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
import com.ticketdesk.authservice.service.impl.AuthServiceImpl;
import com.ticketdesk.authservice.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private User mockUser;
    private UserResponse mockUserResponse;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encoded_pass")
                .roles(Set.of(Role.ROLE_USER))
                .build();

        mockUserResponse = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .roles(Set.of(Role.ROLE_USER))
                .build();
    }

    @Test
    @DisplayName("registerUser: Should register new USER role successfully")
    void registerUser_Success() {
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "Password@123", Role.ROLE_USER);

        given(userRepository.existsByUsername("testuser")).willReturn(false);
        given(userRepository.existsByEmail("test@example.com")).willReturn(false);
        given(userMapper.registerRequestToUser(request)).willReturn(mockUser);
        given(passwordEncoder.encode("Password@123")).willReturn("encoded_pass");
        given(userRepository.save(any(User.class))).willReturn(mockUser);
        given(userMapper.userToUserResponse(mockUser)).willReturn(mockUserResponse);

        UserResponse response = authService.registerUser(request);

        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("testuser");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("registerUser: Should throw AdminRegistrationNotAllowedException when trying to register ROLE_ADMIN")
    void registerUser_ProhibitAdminRegistration() {
        RegisterRequest request = new RegisterRequest("admin_test", "admin@example.com", "Password@123", Role.ROLE_ADMIN);

        assertThatThrownBy(() -> authService.registerUser(request))
                .isInstanceOf(AdminRegistrationNotAllowedException.class)
                .hasMessageContaining("Admin role cannot be self-registered via public endpoint");
    }

    @Test
    @DisplayName("registerUser: Should throw UserAlreadyExistsException when username is taken")
    void registerUser_DuplicateUsername() {
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "Password@123", Role.ROLE_USER);

        given(userRepository.existsByUsername("testuser")).willReturn(true);

        assertThatThrownBy(() -> authService.registerUser(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("Username 'testuser' is already taken");
    }

    @Test
    @DisplayName("loginUser: Should return JWT AuthResponse when credentials are valid")
    void loginUser_Success() {
        LoginRequest request = new LoginRequest("testuser", "Password@123");

        given(userRepository.findByUsernameOrEmail("testuser", "testuser")).willReturn(Optional.of(mockUser));
        given(passwordEncoder.matches("Password@123", "encoded_pass")).willReturn(true);
        given(jwtUtil.generateToken(mockUser)).willReturn("mock_jwt_token");

        AuthResponse response = authService.loginUser(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mock_jwt_token");
        assertThat(response.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("loginUser: Should throw InvalidCredentialsException when password is incorrect")
    void loginUser_InvalidPassword() {
        LoginRequest request = new LoginRequest("testuser", "WrongPassword");

        given(userRepository.findByUsernameOrEmail("testuser", "testuser")).willReturn(Optional.of(mockUser));
        given(passwordEncoder.matches("WrongPassword", "encoded_pass")).willReturn(false);

        assertThatThrownBy(() -> authService.loginUser(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    @DisplayName("getUserById: Should return user details when ID exists")
    void getUserById_Success() {
        given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
        given(userMapper.userToUserResponse(mockUser)).willReturn(mockUserResponse);

        UserResponse response = authService.getUserById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getUserById: Should throw ResourceNotFoundException when user ID does not exist")
    void getUserById_NotFound() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with ID: 99");
    }

    @Test
    @DisplayName("getAgents: Should return list of users with ROLE_AGENT")
    void getAgents_Success() {
        User agentUser = User.builder().id(2L).username("agent1").roles(Set.of(Role.ROLE_AGENT)).build();
        UserResponse agentResponse = UserResponse.builder().id(2L).username("agent1").roles(Set.of(Role.ROLE_AGENT)).build();

        given(userRepository.findByRolesContaining(Role.ROLE_AGENT)).willReturn(List.of(agentUser));
        given(userMapper.userToUserResponse(agentUser)).willReturn(agentResponse);

        List<UserResponse> agents = authService.getAgents();

        assertThat(agents).hasSize(1);
        assertThat(agents.get(0).getUsername()).isEqualTo("agent1");
    }
}
