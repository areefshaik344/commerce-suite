package com.commercesuite.orders.service;
import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.orders.entity.Order;
import com.commercesuite.rbac.entity.AppRole;
import org.springframework.stereotype.Component;

@Component
public class OrderOwnershipGuard {
  public void requireCustomerOrAdmin(Order order, ActorContext actor) {
    if (actor.hasRole(AppRole.ADMIN.name()) || actor.hasRole(AppRole.SUPER_ADMIN.name())) return;
    if (!order.getCustomerId().equals(actor.userId()))
      throw AppException.forbidden("Not your order");
  }
}
