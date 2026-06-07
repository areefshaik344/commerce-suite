package com.commercesuite.payouts.service;

import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.payouts.entity.VendorPayout;
import com.commercesuite.rbac.entity.AppRole;
import org.springframework.stereotype.Component;

@Component
public class PayoutOwnershipGuard {
  public void requireVendorOrAdmin(VendorPayout p, ActorContext actor) {
    if (isAdmin(actor)) return;
    if (!p.getVendorId().equals(actor.userId()))
      throw AppException.forbidden("Not your payout");
  }
  public boolean isAdmin(ActorContext a) {
    return a.hasRole(AppRole.ADMIN.name()) || a.hasRole(AppRole.SUPER_ADMIN.name())
        || a.hasRole(AppRole.FINANCE_ADMIN.name());
  }
}