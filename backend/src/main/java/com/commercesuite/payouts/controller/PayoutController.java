package com.commercesuite.payouts.controller;

import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.api.PageResponse;
import com.commercesuite.common.audit.ActorContextHolder;
import com.commercesuite.payouts.dto.VendorPayoutDto;
import com.commercesuite.payouts.service.PayoutService;
import com.commercesuite.rbac.service.Permissions;
import com.commercesuite.rbac.service.RequiresPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Vendor Payouts")
@RestController
@RequestMapping("/api/v1/vendor/payouts")
@RequiredArgsConstructor
@RequiresPermission(Permissions.VIEW_VENDOR_PAYOUTS)
public class PayoutController {
  private final PayoutService service;
  private final ActorContextHolder actor;

  @GetMapping
  public ApiResponse<PageResponse<VendorPayoutDto>> mine(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    var p = service.listForVendor(actor.require().userId(),
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    return ApiResponse.ok(PageResponse.of(p.map(VendorPayoutDto::from)));
  }

  @GetMapping("/{id}")
  public ApiResponse<VendorPayoutDto> get(@PathVariable UUID id) {
    return ApiResponse.ok(VendorPayoutDto.from(service.get(id, actor.require())));
  }
}