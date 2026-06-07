package com.commercesuite.refunds.controller;
import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.api.PageResponse;
import com.commercesuite.common.audit.ActorContextHolder;
import com.commercesuite.rbac.service.Permissions;
import com.commercesuite.rbac.service.RequiresPermission;
import com.commercesuite.refunds.dto.*;
import com.commercesuite.refunds.service.RefundService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Refunds")
@RestController
@RequestMapping("/api/v1/admin/refunds")
@RequiredArgsConstructor
@RequiresPermission(Permissions.MANAGE_PAYOUTS)
public class RefundController {
  private final RefundService service;
  private final ActorContextHolder actor;

  @GetMapping
  public ApiResponse<PageResponse<RefundRequestDto>> list(@RequestParam(defaultValue="0") int page,
                                                          @RequestParam(defaultValue="20") int size) {
    return ApiResponse.ok(PageResponse.of(service.listAll(
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestedAt")))));
  }

  @GetMapping("/{id}")
  public ApiResponse<RefundRequestDto> get(@PathVariable UUID id) {
    return ApiResponse.ok(service.get(id));
  }

  @PostMapping("/{id}/approve")
  public ApiResponse<RefundRequestDto> approve(@PathVariable UUID id) {
    return ApiResponse.ok(service.approve(id, actor.require()), "Approved");
  }

  @PostMapping("/{id}/reject")
  public ApiResponse<RefundRequestDto> reject(@PathVariable UUID id,
                                              @RequestBody(required=false) RefundDecisionRequest req) {
    return ApiResponse.ok(service.reject(id, req, actor.require()), "Rejected");
  }

  @PostMapping("/{id}/complete")
  public ApiResponse<RefundRequestDto> complete(@PathVariable UUID id) {
    return ApiResponse.ok(service.markCompleted(id, actor.require()), "Completed");
  }
}
