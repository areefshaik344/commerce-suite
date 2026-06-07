package com.commercesuite.notifications.controller;

import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.notifications.dto.NotificationTemplateDto;
import com.commercesuite.notifications.service.NotificationTemplateService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notification Templates (Admin)")
@RestController
@RequestMapping("/api/v1/notification-templates")
@RequiredArgsConstructor
public class NotificationTemplateController {

    private final NotificationTemplateService svc;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<NotificationTemplateDto>> list() {
        return ApiResponse.ok(svc.listAll().stream().map(NotificationTemplateDto::from).toList());
    }
}