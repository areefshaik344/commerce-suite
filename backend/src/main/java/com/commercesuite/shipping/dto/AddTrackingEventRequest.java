package com.commercesuite.shipping.dto;
import jakarta.validation.constraints.*;
public record AddTrackingEventRequest(@NotBlank @Size(max=64) String eventType,
                                      @Size(max=500) String description,
                                      @Size(max=255) String location) {}
