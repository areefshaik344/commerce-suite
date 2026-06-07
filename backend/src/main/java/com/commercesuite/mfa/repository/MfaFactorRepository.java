package com.commercesuite.mfa.repository;

import com.commercesuite.mfa.domain.MfaFactor;
import com.commercesuite.mfa.domain.MfaFactorStatus;
import com.commercesuite.mfa.domain.MfaFactorType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MfaFactorRepository extends JpaRepository<MfaFactor, UUID> {
    Optional<MfaFactor> findByUserIdAndType(UUID userId, MfaFactorType type);
    boolean existsByUserIdAndStatus(UUID userId, MfaFactorStatus status);
}
