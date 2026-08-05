package com.ticketdesk.ticketservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtHeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String userIdStr = request.getHeader("X-User-Id");
        String username = request.getHeader("X-User-Name");
        String rolesStr = request.getHeader("X-User-Roles");

        if (StringUtils.hasText(userIdStr) && StringUtils.hasText(username)) {
            try {
                Long userId = Long.parseLong(userIdStr);
                List<SimpleGrantedAuthority> authorities = List.of();

                if (StringUtils.hasText(rolesStr)) {
                    // Clean up square brackets if present e.g. [ROLE_ADMIN, ROLE_USER]
                    String cleanedRoles = rolesStr.replaceAll("[\\[\\]]", "");
                    authorities = Arrays.stream(cleanedRoles.split(","))
                            .map(String::trim)
                            .filter(StringUtils::hasText)
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, username, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authenticated user {} (ID: {}) with authorities: {}", username, userId, authorities);
            } catch (Exception e) {
                log.error("Failed to parse user identity headers from gateway: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
