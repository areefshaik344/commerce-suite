package com.commercesuite.rbac.service;

import com.commercesuite.rbac.entity.AppRole;
import com.commercesuite.rbac.entity.UserRole;
import com.commercesuite.rbac.repository.UserPermissionOverrideRepository;
import com.commercesuite.rbac.repository.UserRoleRepository;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final UserRoleRepository userRoleRepository;
    private final UserPermissionOverrideRepository overrideRepo;
    private final PermissionCatalog catalog;

    @Transactional(readOnly = true)
    public Set<AppRole> rolesOf(UUID userId) {
        return userRoleRepository.findByUserId(userId).stream()
                .map(UserRole::getRole).collect(Collectors.toCollection(() -> EnumSet.noneOf(AppRole.class)));
    }

    @Transactional(readOnly = true)
    public Set<String> permissionsOf(UUID userId) {
        var roles = rolesOf(userId);
        var overrides = overrideRepo.findByUserId(userId).stream()
                .map(o -> o.getPermission()).toList();
        return catalog.effectivePermissions(roles, overrides);
    }

    @Transactional
    public void grant(UUID userId, AppRole role, UUID grantedBy) {
        if (userRoleRepository.existsByUserIdAndRole(userId, role)) return;
        userRoleRepository.save(UserRole.builder().userId(userId).role(role).grantedBy(grantedBy).build());
    }

    @Transactional
    public void revoke(UUID userId, AppRole role) {
        userRoleRepository.deleteByUserIdAndRole(userId, role);
    }

    public boolean has(UUID userId, AppRole role) {
        return userRoleRepository.existsByUserIdAndRole(userId, role);
    }
}
