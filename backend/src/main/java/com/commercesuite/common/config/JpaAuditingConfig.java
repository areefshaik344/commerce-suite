package com.commercesuite.common.config;

import com.commercesuite.common.audit.ActorContextHolder;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@RequiredArgsConstructor
public class JpaAuditingConfig {
    private final ActorContextHolder actorHolder;

    @Bean
    public AuditorAware<UUID> auditorAware() {
        return () -> {
            var actor = actorHolder.current();
            return actor == null ? Optional.empty() : Optional.ofNullable(actor.userId());
        };
    }
}
