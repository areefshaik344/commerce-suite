package com.commercesuite.payments.service;

import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.payments.entity.PaymentIntent;
import com.commercesuite.rbac.entity.AppRole;
import org.springframework.stereotype.Component;

@Component
public class PaymentOwnershipGuard {
  public void requireCustomerOrAdmin(PaymentIntent intent, ActorContext actor) {
    if (isAdmin(actor)) return;
    if (!intent.getCustomerId().equals(actor.userId()))
      throw AppException.forbidden("Not your payment");
  }
  public boolean isAdmin(ActorContext a) {
    return a.hasRole(AppRole.ADMIN.name()) || a.hasRole(AppRole.SUPER_ADMIN.name())
        || a.hasRole(AppRole.FINANCE_ADMIN.name()) || a.hasRole(AppRole.SUPPORT_ADMIN.name());
  }
}