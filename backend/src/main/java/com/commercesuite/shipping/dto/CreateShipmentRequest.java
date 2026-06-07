package com.commercesuite.shipping.dto;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;
public record CreateShipmentRequest(@NotNull UUID vendorOrderId,
                                    @NotEmpty List<ShipmentItemSpec> items,
                                    String carrier, String trackingNumber, String shippingMethod,
                                    Long shippingPaise) {
  public record ShipmentItemSpec(@NotNull UUID orderItemId, @Min(1) int qty) {}
}
