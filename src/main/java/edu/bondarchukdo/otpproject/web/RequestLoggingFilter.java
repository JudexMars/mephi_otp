package edu.bondarchukdo.otpproject.web;

import java.io.IOException;
import java.util.UUID;

import edu.bondarchukdo.otpproject.security.JwtPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 20)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private static final String MDC_REQUEST_ID = "requestId";
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_ROLE = "role";

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static AuthContext resolveAuthContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtPrincipal principal) {
            return new AuthContext(String.valueOf(principal.userId()), principal.role().name());
        }
        return new AuthContext("anonymous", "-");
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/swagger-ui.html");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        MDC.put(MDC_REQUEST_ID, requestId);
        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            int status = response.getStatus();
            AuthContext auth = resolveAuthContext();
            MDC.put(MDC_USER_ID, auth.userId());
            MDC.put(MDC_ROLE, auth.role());

            log.info(
                    "api method={} uri={} query={} status={} durationMs={} userId={} role={} client={} requestId={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    nullToDash(request.getQueryString()),
                    status,
                    durationMs,
                    auth.userId(),
                    auth.role(),
                    request.getRemoteAddr(),
                    requestId);

            if (status >= 400) {
                log.warn(
                        "api error method={} uri={} status={} userId={} role={} requestId={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        status,
                        auth.userId(),
                        auth.role(),
                        requestId);
            } else {
                log.debug(
                        "api detail userAgent={} contentLength={} requestId={}",
                        request.getHeader("User-Agent"),
                        request.getContentLengthLong(),
                        requestId);
            }

            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_ROLE);
        }
    }

    private record AuthContext(String userId, String role) {
    }
}
