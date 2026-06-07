package com.commercesuite.notifications.preferences;

import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.audit.ActorContextHolder;
import com.commercesuite.notifications.preferences.dto.PreferenceEntryDto;
import com.commercesuite.notifications.preferences.dto.UpdatePreferencesRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notification Preferences")
@RestController
@RequestMapping("/api/v1/me/notification-preferences")
@RequiredArgsConstructor
public class NotificationPreferenceController {

    private final NotificationPreferenceService svc;
    private final ActorContextHolder actorHolder;

    @GetMapping
    public ApiResponse<List<PreferenceEntryDto>> list() {
        return ApiResponse.ok(svc.listFor(actorHolder.require().userId()));
    }

    @PutMapping
    public ApiResponse<List<PreferenceEntryDto>> update(@Valid @RequestBody UpdatePreferencesRequest req) {
        return ApiResponse.ok(svc.upsert(actorHolder.require().userId(), req), "Preferences updated");
    }
}