package com.commercesuite.common.dlq;

import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.dlq.DeadLetterReplayService.Channel;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/dlq")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class DlqAdminController {
    private final DeadLetterReplayService svc;
    public DlqAdminController(DeadLetterReplayService svc) { this.svc = svc; }

    @GetMapping("/{channel}/count")
    public ApiResponse<Long> count(@PathVariable Channel channel) { return ApiResponse.ok(svc.count(channel)); }

    @PostMapping("/{channel}/replay")
    public ApiResponse<Map<String,Integer>> replayAll(@PathVariable Channel channel) {
        return ApiResponse.ok(Map.of("requeued", svc.replayAll(channel)));
    }

    @PostMapping("/{channel}/replay/{id}")
    public ApiResponse<Map<String,Integer>> replayOne(@PathVariable Channel channel, @PathVariable String id) {
        return ApiResponse.ok(Map.of("requeued", svc.replayOne(channel, id)));
    }
}
