package com.commercesuite.coupon.repository;

import com.commercesuite.coupon.entity.Coupon;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {
    Optional<Coupon> findByCodeIgnoreCase(String code);

    /** BLOCKER B-04: pessimistic write lock on the coupon row to serialise
     *  concurrent usage-cap checks. Callers MUST be inside a write tx. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coupon c where lower(c.code) = lower(:code)")
    Optional<Coupon> findByCodeForUpdate(@Param("code") String code);
}