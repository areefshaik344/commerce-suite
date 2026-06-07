package com.commercesuite.returns.controller;
import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.api.PageResponse;
import com.commercesuite.common.audit.ActorContextHolder;
import com.commercesuite.rbac.service.Permissions;
import com.commercesuite.rbac.service.RequiresPermission;
import com.commercesuite.returns.dto.*;
import com.commercesuite.returns.service.ReturnService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Returns")
@RestController
@RequestMapping("/api/v1/returns")
@RequiredArgsConstructor
public class ReturnController {
  private final ReturnService service;
  private final ActorContextHolder actor;

  @PostMapping
  @RequiresPermission(Permissions.PLACE_ORDER)
  public ResponseEntity<ApiResponse<ReturnRequestDto>> create(@Valid @RequestBody CreateReturnRequest req) {
    var out = service.create(req, actor.require());
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(out, "Return requested"));
  }

  @GetMapping
  public ApiResponse<PageResponse<ReturnRequestDto>> mine(@RequestParam(defaultValue="0") int page,
                                                          @RequestParam(defaultValue="20") int size) {
    var p = service.listForCustomer(actor.require().userId(),
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestedAt")));
    return ApiResponse.ok(PageResponse.of(p));
  }

  @GetMapping("/{id}")
  public ApiResponse<ReturnRequestDto> get(@PathVariable UUID id) {
    return ApiResponse.ok(service.get(id, actor.require()));
  }

  @PostMapping("/{id}/receive")
  @RequiresPermission(Permissions.MANAGE_VENDOR_RETURNS)
  public ApiResponse<ReturnRequestDto> receive(@PathVariable UUID id) {
    return ApiResponse.ok(service.markReceived(id, actor.require()), "Received");
  }

  @PostMapping("/{id}/complete")
  @RequiresPermission(Permissions.MANAGE_VENDOR_RETURNS)
  public ApiResponse<ReturnRequestDto> complete(@PathVariable UUID id) {
    return ApiResponse.ok(service.complete(id, actor.require()), "Completed");
  }

  @PostMapping("/{id}/reject")
  @RequiresPermission(Permissions.MANAGE_VENDOR_RETURNS)
  public ApiResponse<ReturnRequestDto> reject(@PathVariable UUID id,
                                              @RequestBody(required=false) ReturnDecisionRequest req) {
    return ApiResponse.ok(service.reject(id, req, actor.require()), "Rejected");
  }
}
