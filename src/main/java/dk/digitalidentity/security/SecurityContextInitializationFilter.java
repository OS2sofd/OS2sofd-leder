package dk.digitalidentity.security;

import java.io.IOException;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Order(1) // Execute early in the filter chain
@Slf4j
public class SecurityContextInitializationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Initialize security context for authenticated users
        if (request.getUserPrincipal() != null) {
            try {
                SecurityUtil.getCurrentContext();
            } catch (Exception e) {
                // Log error but continue - let the actual authorization handle it
                log.error("Failed to initialize security context", e);
            }
        }

        filterChain.doFilter(request, response);
    }
}