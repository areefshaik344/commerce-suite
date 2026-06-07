package com.commercesuite.payments.service;

import com.commercesuite.payments.entity.PaymentAttempt;
import com.commercesuite.payments.entity.PaymentStatus;
import com.commercesuite.payments.repository.PaymentAttemptRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentAttemptService {
  private final PaymentAttemptRepository repo;
  private final Clock clock;

  @Transactional
  public PaymentAttempt record(UUID intentId, PaymentStatus status, String provider,
                               String gatewayRef, String requestJson, String responseJson,
                               String failureCode, String failureMessage) {
    int next = (int) (repo.countByIntentId(intentId) + 1);
    return repo.save(PaymentAttempt.builder()
        .intentId(intentId).attemptNumber(next).status(status)
        .gatewayProvider(provider).gatewayReference(gatewayRef)
        .requestPayload(requestJson == null ? "{}" : requestJson)
        .responsePayload(responseJson == null ? "{}" : responseJson)
        .failureCode(failureCode).failureMessage(failureMessage)
        .attemptedAt(Instant.now(clock)).build());
  }

  @Transactional(readOnly = true)
  public List<PaymentAttempt> list(UUID intentId) {
    return repo.findByIntentIdOrderByAttemptNumberAsc(intentId);
  }
}