package com.commercesuite.payouts.controller;

import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.api.PageResponse;
import com.commercesuite.common.audit.ActorContextHolder;
import com.commercesuite.payouts.dto.GeneratePayoutBatchRequest;
import com.commercesuite.payouts.dto.PayoutBatchDto;
import com.commercesuite.payouts.dto.VendorPayoutDto;
import com.commercesuite.payouts.service.PayoutBatchService;
import com.commercesuite.payouts.service.PayoutService;
import com.commercesuite.rbac.service.Permissions;
import com.commercesuite.rbac.service.RequiresPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Payouts")
@RestController
@RequestMapping("/api/v1/admin/payouts")
@RequiredArgsConstructor
@RequiresPermission(Permissions.MANAGE_PAYOUTS)
public class AdminPayoutController {
  private final PayoutService payouts;
  private final PayoutBatchService batches;
  private final ActorContextHolder actor;

  @GetMapping
  public ApiResponse<PageResponse<VendorPayoutDto>> all(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    var p = payouts.listAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    return ApiResponse.ok(PageResponse.of(p.map(VendorPayoutDto::from)));
  }

  @PostMapping("/generate")
  public ApiResponse<PayoutBatchDto> generate(@RequestBody(required = false) GeneratePayoutBatchRequest req) {
    var batch = batches.generate(req == null ? null : req.notes(), actor.require());
    return ApiResponse.ok(PayoutBatchDto.from(batch), "Batch generated");
  }

  @PostMapping("/{id}/process")
  public ApiResponse<VendorPayoutDto> process(@PathVariable UUID id) {
    return ApiResponse.ok(VendorPayoutDto.from(payouts.markProcessing(id, actor.require())));
  }

  @PostMapping("/{id}/complete")
  public ApiResponse<VendorPayoutDto> complete(@PathVariable UUID id,
      @RequestBody(required = false) Map<String,String> body) {
    String bankRef = body == null ? null : body.get("bankReference");
    return ApiResponse.ok(VendorPayoutDto.from(payouts.markCompleted(id, bankRef, actor.require())),
        "Completed");
  }

  @PostMapping("/{id}/fail")
  public ApiResponse<VendorPayoutDto> fail(@PathVariable UUID id,
      @RequestBody(required = false) Map<String,String> body) {
    String code = body == null ? null : body.get("code");
    String msg  = body == null ? null : body.get("message");
    return ApiResponse.ok(VendorPayoutDto.from(payouts.markFailed(id, code, msg, actor.require())),
        "Failed");
  }
}