package com.commercesuite.common.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    /** path-prefix matched, method, policy */
    private record Rule(String method, String prefix, RateLimitPolicy policy) {}

    private static final List<Rule> RULES = List.of(
        new Rule("POST", "/api/v1/auth/login",            RateLimitPolicy.of("auth.login",   5,  60)),
        new Rule("POST", "/api/v1/auth/refresh",          RateLimitPolicy.of("auth.refresh", 20, 60)),
        new Rule("POST", "/api/v1/auth/password/forgot",  RateLimitPolicy.of("auth.forgot",  3, 3600)),
        new Rule("POST", "/api/v1/auth/password/reset",   RateLimitPolicy.of("auth.reset",   5, 3600)),
        new Rule("POST", "/api/v1/auth/email/verify",     RateLimitPolicy.of("auth.verify",  5,  600)),
        new Rule("POST", "/api/v1/auth/mfa/verify",       RateLimitPolicy.of("mfa.verify",  10,  300)),
        new Rule("*",    "/api/v1/admin/webhooks",        RateLimitPolicy.of("admin.webhooks", 60, 60)),
        new Rule("*",    "/api/v1/admin/",                RateLimitPolicy.of("admin.api",   120, 60))
    );

    private final RateLimitService service;
    private final ObjectMapper mapper;

    public RateLimitFilter(RateLimitService service, ObjectMapper mapper) {
        this.service = service; this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String path = req.getRequestURI();
        String method = req.getMethod();
        for (Rule r : RULES) {
            if ((r.method().equals("*") || r.method().equalsIgnoreCase(method))
                    && path.startsWith(r.prefix())) {
                String key = r.policy().name() + ":" + clientKey(req);
                if (!service.tryAcquire(key, r.policy())) {
                    res.setStatus(429);
                    res.setHeader("Retry-After", String.valueOf(r.policy().refillWindowSeconds()));
                    res.setContentType("application/json");
                    res.getWriter().write(mapper.writeValueAsString(Map.of(
                        "success", false,
                        "error", Map.of("code", "RATE_LIMITED", "message", "Too many requests"))));
                    return;
                }
                break;
            }
        }
        chain.doFilter(req, res);
    }

    private static String clientKey(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        String ip = (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : req.getRemoteAddr();
        String actor = req.getHeader("X-Actor-Id");
        return ip + "|" + (actor == null ? "anon" : actor);
    }
}
