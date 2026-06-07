package com.commercesuite.settlement.controller;

import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.api.PageResponse;
import com.commercesuite.common.audit.ActorContextHolder;
import com.commercesuite.rbac.service.Permissions;
import com.commercesuite.rbac.service.RequiresPermission;
import com.commercesuite.settlement.dto.SettlementDto;
import com.commercesuite.settlement.service.SettlementService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Vendor Settlements")
@RestController
@RequestMapping("/api/v1/vendor/settlements")
@RequiredArgsConstructor
@RequiresPermission(Permissions.VIEW_VENDOR_FINANCIALS)
public class SettlementController {
  private final SettlementService service;
  private final ActorContextHolder actor;

  @GetMapping
  public ApiResponse<PageResponse<SettlementDto>> mine(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    var p = service.listForVendor(actor.require().userId(),
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    return ApiResponse.ok(PageResponse.of(p));
  }

  @GetMapping("/{id}")
  public ApiResponse<SettlementDto> get(@PathVariable UUID id) {
    return ApiResponse.ok(service.get(id, actor.require()));
  }
}