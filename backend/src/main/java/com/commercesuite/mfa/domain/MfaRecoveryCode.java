package com.commercesuite.mfa.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity @Table(name = "mfa_recovery_codes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MfaRecoveryCode {
    @Id @GeneratedValue private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "code_hash", nullable = false) private String codeHash;
    @Column(name = "used_at") private Instant usedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
}
