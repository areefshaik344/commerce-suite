package com.commercesuite.returns.dto;
import com.commercesuite.returns.entity.ReturnReason;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;
public record CreateReturnRequest(@NotNull UUID vendorOrderId,
                                  @NotEmpty List<ReturnItemSpec> items,
                                  @NotNull ReturnReason reason,
                                  @Size(max=1000) String note,
                                  UUID pickupAddressId) {
  public record ReturnItemSpec(@NotNull UUID orderItemId, @Min(1) int qty) {}
}
