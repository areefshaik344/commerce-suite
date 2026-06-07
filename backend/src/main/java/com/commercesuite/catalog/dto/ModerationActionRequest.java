package com.commercesuite.catalog.dto;

import jakarta.validation.constraints.Size;

public record ModerationActionRequest(@Size(max = 2000) String reason) {}