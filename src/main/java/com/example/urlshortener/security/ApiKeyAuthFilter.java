package com.example.urlshortener.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.urlshortener.api.error.ErrorCode;
import com.example.urlshortener.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Shared-secret API key auth for create, bulk, disable, and analytics APIs.
 * Header: {@code X-API-Key}. Redirects remain public.
 */
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-Key";

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthFilter(AppProperties appProperties, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!appProperties.getSecurity().isRequireApiKey()) {
            return true;
        }
        return !requiresApiKey(request);
    }

    static boolean requiresApiKey(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if (path.startsWith("/api/v1/analytics")) {
            return true;
        }
        if (path.startsWith("/api/v1/urls")) {
            return "POST".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method);
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String expected = appProperties.getSecurity().getApiKey();
        if (expected == null || expected.isBlank()) {
            writeUnauthorized(response, request, "API key is not configured");
            return;
        }
        String provided = request.getHeader(API_KEY_HEADER);
        if (provided == null || !constantTimeEquals(expected, provided)) {
            writeUnauthorized(response, request, "Missing or invalid API key");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, HttpServletRequest request, String message)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        var body = new AuthErrorBody(
                Instant.now().toString(),
                401,
                ErrorCode.UNAUTHORIZED.name(),
                message,
                request.getRequestURI(),
                request.getHeader("X-Request-Id"));
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private static boolean constantTimeEquals(String expected, String provided) {
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = provided.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }

    private record AuthErrorBody(
            String timestamp, int status, String errorCode, String message, String path, String requestId) {
    }
}
