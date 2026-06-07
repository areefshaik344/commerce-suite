package com.commercesuite.payments.controller;

import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.api.PageResponse;
import com.commercesuite.common.audit.ActorContextHolder;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.common.idempotency.IdempotencyService;
import com.commercesuite.payments.dto.*;
import com.commercesuite.payments.entity.PaymentIntent;
import com.commercesuite.payments.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payments")
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
  private final PaymentService paymentService;
  private final IdempotencyService idempotency;
  private final ActorContextHolder actor;
  private final ObjectMapper json;

  @PostMapping("/intents")
  public ResponseEntity<ApiResponse<PaymentIntentDto>> createIntent(
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @Valid @RequestBody CreatePaymentIntentRequest req) throws Exception {
    if (key == null || key.isBlank())
      throw AppException.badRequest(ErrorCode.VALIDATION_FAILED, "Idempotency-Key header required");
    var a = actor.require();
    String canonical = json.writeValueAsString(req);
    var cached = idempotency.replayOrExecute(a.userId(), "POST /payments/intents", key, canonical, () -> {
      try {
        PaymentIntent created = paymentService.createIntent(req, key, a);
        PaymentIntentDto dto = PaymentIntentDto.from(created);
        String body = json.writeValueAsString(ApiResponse.ok(dto, "Payment intent created"));
        return new IdempotencyService.CachedResponse(HttpStatus.CREATED.value(), body, false);
      } catch (Exception e) { throw new RuntimeException(e); }
    });
    return ResponseEntity.status(cached.status())
        .header("Idempotent-Replay", String.valueOf(cached.replayed()))
        .header("Content-Type", "application/json")
        .body(json.readValue(cached.body(), ApiResponse.class));
  }

  @GetMapping
  public ApiResponse<PageResponse<PaymentIntentDto>> mine(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    var p = paymentService.listMine(actor.require(),
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    return ApiResponse.ok(PageResponse.of(p.map(PaymentIntentDto::from)));
  }

  @GetMapping("/{id}")
  public ApiResponse<PaymentIntentDto> get(@PathVariable UUID id) {
    return ApiResponse.ok(PaymentIntentDto.from(paymentService.loadOwned(id, actor.require())));
  }

  @PostMapping("/{id}/retry")
  public ApiResponse<PaymentIntentDto> retry(@PathVariable UUID id,
      @RequestBody(required = false) RetryPaymentRequest req) {
    return ApiResponse.ok(PaymentIntentDto.from(paymentService.retry(id, req, actor.require())),
        "Retried");
  }

  @PostMapping("/{id}/confirm")
  public ApiResponse<PaymentIntentDto> confirm(@PathVariable UUID id) {
    return ApiResponse.ok(PaymentIntentDto.from(paymentService.confirmCapture(id, actor.require())),
        "Captured");
  }

  @PostMapping("/{id}/cancel")
  public ApiResponse<PaymentIntentDto> cancel(@PathVariable UUID id,
      @RequestBody(required = false) java.util.Map<String,String> body) {
    String reason = body == null ? null : body.get("reason");
    return ApiResponse.ok(PaymentIntentDto.from(paymentService.cancel(id, reason, actor.require())),
        "Cancelled");
  }
}