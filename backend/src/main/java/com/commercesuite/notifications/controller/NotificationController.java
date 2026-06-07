package com.commercesuite.notifications.controller;

import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.audit.ActorContextHolder;
import com.commercesuite.notifications.dto.NotificationDto;
import com.commercesuite.notifications.service.NotificationInboxService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notifications")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationInboxService inbox;
    private final ActorContextHolder actor;

    @GetMapping
    public ApiResponse<List<NotificationDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(inbox.list(actor.require().userId(), page, size)
                .stream().map(NotificationDto::from).toList());
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unread() {
        return ApiResponse.ok(Map.of("count", inbox.unreadCount(actor.require().userId())));
    }

    @PostMapping("/{id}/mark-read")
    public ApiResponse<NotificationDto> markRead(@PathVariable UUID id) {
        return ApiResponse.ok(NotificationDto.from(inbox.markRead(actor.require().userId(), id)));
    }

    @PostMapping("/mark-all-read")
    public ApiResponse<Map<String, Integer>> markAll() {
        int n = inbox.markAllRead(actor.require().userId());
        return ApiResponse.ok(Map.of("updated", n));
    }
}