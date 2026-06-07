package com.commercesuite.shipping.dto;
import com.commercesuite.shipping.entity.ShipmentStatus;
import jakarta.validation.constraints.NotNull;
public record UpdateShipmentStatusRequest(@NotNull ShipmentStatus status, String note) {}
