package com.commercesuite.user.controller;

import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.audit.ActorContextHolder;
import com.commercesuite.rbac.service.Permissions;
import com.commercesuite.rbac.service.RequiresPermission;
import com.commercesuite.user.dto.ProfileDto;
import com.commercesuite.user.dto.UpdateProfileRequest;
import com.commercesuite.user.service.ProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Profile")
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService profileService;
    private final ActorContextHolder actor;

    @GetMapping
    @RequiresPermission(Permissions.MANAGE_OWN_PROFILE)
    public ApiResponse<ProfileDto> me() { return ApiResponse.ok(profileService.getMe(actor.require().userId())); }

    @PatchMapping
    @RequiresPermission(Permissions.MANAGE_OWN_PROFILE)
    public ApiResponse<ProfileDto> update(@Valid @RequestBody UpdateProfileRequest req) {
        return ApiResponse.ok(profileService.updateMe(actor.require().userId(), req), "Profile updated");
    }
}
