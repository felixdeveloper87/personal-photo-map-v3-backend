package com.personalphotomap.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Filter responsible for authenticating incoming HTTP requests using JWT
 * tokens.
 * This filter runs once per request and sets the authenticated user in the
 * Spring Security context if the token is valid.
 */

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * Constructor-based injection for JwtUtil and CustomUserDetailsService.
     */
    public JwtAuthenticationFilter(JwtUtil jwtUtil, CustomUserDetailsService customUserDetailsService) {
        this.jwtUtil = jwtUtil;
        this.customUserDetailsService = customUserDetailsService;
    }

    /**
     * List of public endpoints that should be excluded from JWT authentication.
     */
    private static final List<String> EXCLUDE_URLS = Arrays.asList(
            "/api/auth/register",
            "/api/auth/login",
            "/photomap",
            "/api/other-public-endpoint");

    /**
     * Main logic for intercepting and authenticating requests.
     * If the JWT is valid, it sets the authenticated user in the SecurityContext.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Allow preflight CORS requests (OPTIONS)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String requestPath = request.getRequestURI();

        // Skip authentication for public/excluded endpoints
        if (isExcluded(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract JWT token from Authorization header
        String token = extractJwtFromRequest(request);
        String email = null;

        if (StringUtils.hasText(token)) {
            try {
                email = jwtUtil.extractUsername(token); // Extract user email from JWT token
            } catch (Exception e) {
                logger.error("Failed to extract email from JWT token", e);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT token");
                return;
            }
        }

        // Authenticate user only if not already authenticated
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

            // Validate token and set authenticated user in context
            if (jwtUtil.validateToken(token, userDetails.getUsername())) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Helper method to check if the request path is excluded from authentication.
     *
     * @param path The current request URI
     * @return true if path should be excluded
     */
    private boolean isExcluded(String path) {
        for (String pattern : EXCLUDE_URLS) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper method to extract the JWT token from the Authorization header.
     *
     * @param request HTTP request
     * @return the JWT token without the "Bearer " prefix, or null if not found
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Remove the "Bearer " prefix
        }
        return null;
    }
}
