package com.commercesuite.checkout.repository;

import com.commercesuite.checkout.entity.CheckoutSession;
import com.commercesuite.checkout.entity.CheckoutStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CheckoutSessionRepository extends JpaRepository<CheckoutSession, UUID> {
    Optional<CheckoutSession> findFirstByUserIdAndStatusIn(UUID userId, java.util.Collection<CheckoutStatus> statuses);
    Optional<CheckoutSession> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);

    @Query("""
        select s from CheckoutSession s
        where s.status not in (
              com.commercesuite.checkout.entity.CheckoutStatus.EXPIRED,
              com.commercesuite.checkout.entity.CheckoutStatus.CANCELLED,
              com.commercesuite.checkout.entity.CheckoutStatus.CONVERTED)
          and s.expiresAt <= :cutoff
        order by s.expiresAt asc
        """)
    List<CheckoutSession> findExpired(@Param("cutoff") Instant cutoff, Pageable page);
}