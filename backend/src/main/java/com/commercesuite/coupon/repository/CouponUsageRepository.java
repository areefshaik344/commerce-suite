package com.commercesuite.coupon.repository;

import com.commercesuite.coupon.entity.CouponUsage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, UUID> {
    long countByCouponId(UUID couponId);
    long countByCouponIdAndUserId(UUID couponId, UUID userId);
    List<CouponUsage> findByCheckoutId(UUID checkoutId);
}