package com.commercesuite.settlement.controller;

import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.api.PageResponse;
import com.commercesuite.common.audit.ActorContextHolder;
import com.commercesuite.rbac.service.Permissions;
import com.commercesuite.rbac.service.RequiresPermission;
import com.commercesuite.settlement.dto.CalculateSettlementRequest;
import com.commercesuite.settlement.dto.SettlementDto;
import com.commercesuite.settlement.service.SettlementService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Settlements")
@RestController
@RequestMapping("/api/v1/admin/settlements")
@RequiredArgsConstructor
@RequiresPermission(Permissions.MANAGE_PAYOUTS)
public class AdminSettlementController {
  private final SettlementService service;
  private final ActorContextHolder actor;

  @GetMapping
  public ApiResponse<PageResponse<SettlementDto>> all(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    var p = service.listAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    return ApiResponse.ok(PageResponse.of(p));
  }

  @PostMapping("/calculate")
  public ApiResponse<SettlementDto> calculate(@Valid @RequestBody CalculateSettlementRequest req) {
    var s = service.calculate(req.vendorId(), req.periodStart(), req.periodEnd(), actor.require());
    return ApiResponse.ok(service.toDto(s), "Calculated");
  }

  @PostMapping("/{id}/lock")
  public ApiResponse<SettlementDto> lock(@PathVariable UUID id) {
    return ApiResponse.ok(service.toDto(service.lock(id, actor.require())), "Locked");
  }
}