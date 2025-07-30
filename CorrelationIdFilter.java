package com.example.streambridge.util;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CorrelationIdFilter extends OncePerRequestFilter {
	private static final String MESSAGE_ID_HEADER = "messageId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String messageId = request.getHeader(MESSAGE_ID_HEADER);
        if (messageId == null || messageId.isBlank()) {
            messageId = UUID.randomUUID().toString(); // generate if missing
        }

        MDC.put("messageId", messageId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("messageId");
        }
    }
}