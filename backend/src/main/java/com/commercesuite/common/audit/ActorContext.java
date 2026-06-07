package com.commercesuite.common.audit;

import java.util.Set;
import java.util.UUID;

/** Server-derived from JWT. Mirrors src/types/actor.ts ActorContext (server is authoritative). */
public record ActorContext(UUID userId, Set<String> roles, Set<String> permissions, String requestId) {
    public boolean hasRole(String role)             { return roles != null && roles.contains(role); }
    public boolean hasPermission(String permission) { return permissions != null && permissions.contains(permission); }
    public boolean isAuthenticated()                { return userId != null; }
}
