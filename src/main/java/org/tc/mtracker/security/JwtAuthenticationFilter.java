package org.tc.mtracker.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final HandlerExceptionResolver handlerExceptionResolver;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;



    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    )throws ServletException, IOException {

        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
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
        } catch (JwtException e) {
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