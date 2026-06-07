package com.commercesuite.settlement.service;

import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.rbac.entity.AppRole;
import com.commercesuite.settlement.entity.Settlement;
import org.springframework.stereotype.Component;

@Component
public class SettlementOwnershipGuard {
  public void requireVendorOrAdmin(Settlement s, ActorContext actor) {
    if (isAdmin(actor)) return;
    if (!s.getVendorId().equals(actor.userId()))
      throw AppException.forbidden("Not your settlement");
  }
  public boolean isAdmin(ActorContext a) {
    return a.hasRole(AppRole.ADMIN.name()) || a.hasRole(AppRole.SUPER_ADMIN.name())
        || a.hasRole(AppRole.FINANCE_ADMIN.name());
  }
}