package com.commercesuite.mfa.repository;

import com.commercesuite.mfa.domain.MfaRecoveryCode;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MfaRecoveryCodeRepository extends JpaRepository<MfaRecoveryCode, UUID> {
    List<MfaRecoveryCode> findByUserIdAndUsedAtIsNull(UUID userId);
}
