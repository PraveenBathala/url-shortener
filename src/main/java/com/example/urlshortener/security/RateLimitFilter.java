package com.example.urlshortener.security;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
 * Fixed-window rate limiter for create/bulk/disable abuse prevention (single-instance MVP).
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final Map<String, Deque<Long>> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(AppProperties appProperties, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!appProperties.getSecurity().getRateLimit().isEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        String method = request.getMethod();
        boolean managedWrite = path.startsWith("/api/v1/urls")
                && ("POST".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method));
        return !managedWrite;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        int limit = appProperties.getSecurity().getRateLimit().getRequestsPerMinute();
        String key = clientKey(request);
        long now = System.currentTimeMillis();
        long windowStart = now - 60_000L;

        Deque<Long> timestamps = windows.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= limit) {
                writeTooMany(response, request);
                return;
            }
            timestamps.addLast(now);
        }
        filterChain.doFilter(request, response);
    }

    private static String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private void writeTooMany(HttpServletResponse response, HttpServletRequest request) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        var body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", 429,
                "errorCode", ErrorCode.RATE_LIMIT_EXCEEDED.name(),
                "message", "Rate limit exceeded",
                "path", request.getRequestURI(),
                "requestId", request.getHeader("X-Request-Id") == null ? "" : request.getHeader("X-Request-Id"));
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
