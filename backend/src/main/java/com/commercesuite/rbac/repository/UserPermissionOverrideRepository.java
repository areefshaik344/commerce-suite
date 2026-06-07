package com.commercesuite.rbac.repository;

import com.commercesuite.rbac.entity.UserPermissionOverride;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPermissionOverrideRepository extends JpaRepository<UserPermissionOverride, UUID> {
    List<UserPermissionOverride> findByUserId(UUID userId);
}
