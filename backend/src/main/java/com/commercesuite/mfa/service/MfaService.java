package com.commercesuite.mfa.service;

import com.commercesuite.mfa.domain.*;
import com.commercesuite.mfa.repository.*;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MfaService {
    private static final SecureRandom RNG = new SecureRandom();
    private final MfaFactorRepository factors;
    private final MfaRecoveryCodeRepository codes;
    private final TotpService totp;

    public MfaService(MfaFactorRepository factors, MfaRecoveryCodeRepository codes, TotpService totp) {
        this.factors = factors; this.codes = codes; this.totp = totp;
    }

    public record SetupResponse(String secret, String otpAuthUri) {}
    public record ActivationResponse(List<String> recoveryCodes) {}

    @Transactional
    public SetupResponse beginSetup(UUID userId, String accountLabel) {
        String secret = totp.generateSecret();
        MfaFactor f = factors.findByUserIdAndType(userId, MfaFactorType.TOTP).orElseGet(MfaFactor::new);
        f.setUserId(userId); f.setType(MfaFactorType.TOTP);
        f.setSecretEnc(secret); // production: encrypt with KMS
        f.setStatus(MfaFactorStatus.PENDING);
        f.setLabel(accountLabel);
        if (f.getCreatedAt() == null) f.setCreatedAt(Instant.now());
        factors.save(f);
        return new SetupResponse(secret, totp.otpAuthUri("CommerceSuite", accountLabel, secret));
    }

    @Transactional
    public ActivationResponse activate(UUID userId, String code) {
        MfaFactor f = factors.findByUserIdAndType(userId, MfaFactorType.TOTP)
                .orElseThrow(() -> new IllegalStateException("No pending MFA factor"));
        if (!totp.verify(f.getSecretEnc(), code)) throw new IllegalArgumentException("Invalid code");
        f.setStatus(MfaFactorStatus.ACTIVE);
        f.setActivatedAt(Instant.now());
        factors.save(f);
        List<String> recovery = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            byte[] buf = new byte[5]; RNG.nextBytes(buf);
            String raw = TotpService.base32(buf).substring(0, 10);
            recovery.add(raw);
            codes.save(MfaRecoveryCode.builder().userId(userId)
                .codeHash(BCrypt.hashpw(raw, BCrypt.gensalt(10)))
                .createdAt(Instant.now()).build());
        }
        return new ActivationResponse(recovery);
    }

    @Transactional
    public boolean verify(UUID userId, String code) {
        Optional<MfaFactor> of = factors.findByUserIdAndType(userId, MfaFactorType.TOTP);
        if (of.isEmpty() || of.get().getStatus() != MfaFactorStatus.ACTIVE) return false;
        if (totp.verify(of.get().getSecretEnc(), code)) {
            of.get().setLastUsedAt(Instant.now()); return true;
        }
        for (MfaRecoveryCode rc : codes.findByUserIdAndUsedAtIsNull(userId)) {
            if (BCrypt.checkpw(code, rc.getCodeHash())) {
                rc.setUsedAt(Instant.now()); codes.save(rc); return true;
            }
        }
        return false;
    }

    public boolean isEnabled(UUID userId) {
        return factors.existsByUserIdAndStatus(userId, MfaFactorStatus.ACTIVE);
    }

    @Transactional
    public void disable(UUID userId) {
        factors.findByUserIdAndType(userId, MfaFactorType.TOTP).ifPresent(f -> {
            f.setStatus(MfaFactorStatus.DISABLED); factors.save(f);
        });
    }
}
