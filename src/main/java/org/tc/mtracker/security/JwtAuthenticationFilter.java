package org.tc.mtracker.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.tc.mtracker.utils.exceptions.JwtAuthenticationException;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final HandlerExceptionResolver handlerExceptionResolver;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    private static final List<String> PUBLIC_AUTH_PATHS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/sign-up",
            "/api/v1/auth/refresh",
            "/api/v1/auth/verify",
            "/api/v1/auth/reset-token",
            "/api/v1/auth/getTokenToResetPassword",
            "/api/v1/auth/reset-password/confirm",
            "/api/v1/users/verify-email"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return PUBLIC_AUTH_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) {

        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            handlerExceptionResolver.resolveException(
                    request, response, null,
                    new JwtAuthenticationException(
                            "Missing or invalid Authorization header. Please provide Bearer token.",
                            "missing_token"
                    )
            );
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            final String userEmail = jwtService.extractUsername(jwt);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (userEmail != null && authentication == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                if (!jwtService.isTokenValid(jwt, userDetails)) {
                    handleInvalidToken(request, response, jwt);
                    return;
                }

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

            filterChain.doFilter(request, response);

        } catch (UsernameNotFoundException e) {
            log.debug("User not found: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            handlerExceptionResolver.resolveException(
                    request, response, null,
                    new JwtAuthenticationException("Invalid token", "invalid_token")
            );
        } catch (io.jsonwebtoken.JwtException e) {
            log.debug("JWT processing error: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            handlerExceptionResolver.resolveException(
                    request, response, null,
                    new JwtAuthenticationException("Invalid token: " + e.getMessage(), "invalid_token")
            );
        } catch (Exception e) {
            log.error("Unexpected error during JWT authentication", e);
            SecurityContextHolder.clearContext();
            handlerExceptionResolver.resolveException(
                    request, response, null,
                    new JwtAuthenticationException("Authentication failed", "authentication_error")
            );
        }
    }

    private void handleInvalidToken(
            HttpServletRequest request,
            HttpServletResponse response,
            String jwt
    ) {
        if (jwtService.isTokenExpired(jwt)) {
            handlerExceptionResolver.resolveException(
                    request, response, null,
                    new JwtAuthenticationException(
                            "Token has expired. Please refresh your token.",
                            "expired_token"
                    )
            );
        } else {
            handlerExceptionResolver.resolveException(
                    request, response, null,
                    new JwtAuthenticationException(
                            "Invalid token signature",
                            "invalid_token"
                    )
            );
        }
    }
}