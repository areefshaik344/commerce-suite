package com.commercesuite.mfa.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity @Table(name = "mfa_factors")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MfaFactor {
    @Id @GeneratedValue private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private MfaFactorType type;
    @Column(name = "secret_enc", nullable = false) private String secretEnc;
    private String label;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private MfaFactorStatus status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "activated_at") private Instant activatedAt;
    @Column(name = "last_used_at") private Instant lastUsedAt;
}
