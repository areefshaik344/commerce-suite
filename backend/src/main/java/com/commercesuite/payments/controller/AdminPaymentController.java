package com.commercesuite.payments.controller;

import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.api.PageResponse;
import com.commercesuite.payments.dto.PaymentIntentDto;
import com.commercesuite.payments.dto.PaymentTransactionDto;
import com.commercesuite.payments.service.PaymentService;
import com.commercesuite.rbac.service.Permissions;
import com.commercesuite.rbac.service.RequiresPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Payments")
@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
@RequiresPermission(Permissions.MANAGE_PAYOUTS)
public class AdminPaymentController {
  private final PaymentService paymentService;

  @GetMapping
  public ApiResponse<PageResponse<PaymentIntentDto>> all(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    var p = paymentService.listAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    return ApiResponse.ok(PageResponse.of(p.map(PaymentIntentDto::from)));
  }

  @GetMapping("/{id}/transactions")
  public ApiResponse<List<PaymentTransactionDto>> ledger(@PathVariable UUID id) {
    return ApiResponse.ok(paymentService.transactionsOf(id).stream()
        .map(PaymentTransactionDto::from).toList());
  }
}