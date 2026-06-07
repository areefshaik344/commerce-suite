package com.commercesuite.security.jwt;

import com.commercesuite.common.audit.ActorContext;
import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {
    private final ActorContext actor;
    public JwtAuthenticationToken(ActorContext actor, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.actor = actor;
        setAuthenticated(true);
    }
    public static JwtAuthenticationToken from(ActorContext a) {
        var authorities = a.roles().stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toList());
        a.permissions().forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
        return new JwtAuthenticationToken(a, authorities);
    }
    public ActorContext actor() { return actor; }
    @Override public Object getCredentials() { return null; }
    @Override public Object getPrincipal()   { return actor.userId(); }
}
