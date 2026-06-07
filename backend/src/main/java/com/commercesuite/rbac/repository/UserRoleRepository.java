package com.commercesuite.rbac.repository;

import com.commercesuite.rbac.entity.AppRole;
import com.commercesuite.rbac.entity.UserRole;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {
    List<UserRole> findByUserId(UUID userId);
    boolean existsByUserIdAndRole(UUID userId, AppRole role);
    void deleteByUserIdAndRole(UUID userId, AppRole role);
}
