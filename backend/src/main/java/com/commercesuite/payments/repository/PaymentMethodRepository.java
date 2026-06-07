package com.commercesuite.payments.repository;
import com.commercesuite.payments.entity.PaymentMethod;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {
  List<PaymentMethod> findByCustomerId(UUID customerId);
}