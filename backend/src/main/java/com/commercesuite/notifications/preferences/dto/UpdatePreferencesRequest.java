package com.commercesuite.notifications.preferences.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdatePreferencesRequest(@NotNull @Valid List<PreferenceEntryDto> entries) {}