package com.ticketdesk.authservice.config;

import com.ticketdesk.authservice.entity.User;
import com.ticketdesk.authservice.enums.Role;
import com.ticketdesk.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@ticketdesk.com")
                    .password(passwordEncoder.encode("Admin@1234"))
                    .roles(Set.of(Role.ROLE_ADMIN, Role.ROLE_AGENT, Role.ROLE_USER))
                    .build();
            userRepository.save(admin);
            log.info("Initialized default ADMIN user: admin / Admin@1234");
        }

        if (!userRepository.existsByUsername("agent")) {
            User agent = User.builder()
                    .username("agent")
                    .email("agent@ticketdesk.com")
                    .password(passwordEncoder.encode("Agent@1234"))
                    .roles(Set.of(Role.ROLE_AGENT, Role.ROLE_USER))
                    .build();
            userRepository.save(agent);
            log.info("Initialized default AGENT user: agent / Agent@1234");
        }
    }
}
