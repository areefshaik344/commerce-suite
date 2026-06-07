package com.commercesuite.security.filter;

import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.util.RequestIdFilter;
import com.commercesuite.security.jwt.JwtAuthenticationToken;
import com.commercesuite.security.jwt.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenService tokens;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims c = tokens.parse(token).getPayload();
                if (!"access".equals(c.get("typ"))) throw new JwtException("not an access token");
                UUID userId = UUID.fromString(c.getSubject());
                Set<String> roles = toStringSet(c.get("roles"));
                Set<String> perms = toStringSet(c.get("perms"));
                String rid = (String) req.getAttribute(RequestIdFilter.MDC_KEY);
                if (rid == null) rid = res.getHeader(RequestIdFilter.HEADER);
                ActorContext actor = new ActorContext(userId, roles, perms, rid);
                SecurityContextHolder.getContext().setAuthentication(JwtAuthenticationToken.from(actor));
            } catch (Exception ignore) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(req, res);
    }

    @SuppressWarnings("unchecked")
    private static Set<String> toStringSet(Object o) {
        if (o instanceof Collection<?> c) {
            Set<String> s = new HashSet<>();
            for (Object x : c) if (x != null) s.add(x.toString());
            return s;
        }
        return Set.of();
    }
}
