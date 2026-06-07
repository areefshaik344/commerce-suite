package com.commercesuite.orders.service;
import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.orders.entity.VendorOrder;
import com.commercesuite.rbac.entity.AppRole;
import com.commercesuite.vendor.entity.Vendor;
import com.commercesuite.vendor.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VendorOrderOwnershipGuard {
  private final VendorRepository vendorRepo;

  public void requireVendorOrAdmin(VendorOrder vo, ActorContext actor) {
    if (actor.hasRole(AppRole.ADMIN.name()) || actor.hasRole(AppRole.SUPER_ADMIN.name())) return;
    Vendor v = vendorRepo.findByOwnerUserId(actor.userId())
        .orElseThrow(() -> AppException.forbidden("Not a vendor"));
    if (!v.getId().equals(vo.getVendorId()))
      throw AppException.forbidden("Not your vendor order");
  }
}
