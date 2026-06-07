package com.commercesuite.user.controller;

import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.audit.ActorContextHolder;
import com.commercesuite.rbac.service.Permissions;
import com.commercesuite.rbac.service.RequiresPermission;
import com.commercesuite.user.dto.AddressDto;
import com.commercesuite.user.dto.UpsertAddressRequest;
import com.commercesuite.user.service.AddressService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Addresses")
@RestController
@RequestMapping("/api/v1/me/addresses")
@RequiredArgsConstructor
public class AddressController {
    private final AddressService service;
    private final ActorContextHolder actor;

    @GetMapping
    @RequiresPermission(Permissions.MANAGE_OWN_ADDRESSES)
    public ApiResponse<List<AddressDto>> list() {
        return ApiResponse.ok(service.list(actor.require().userId()));
    }

    @PostMapping
    @RequiresPermission(Permissions.MANAGE_OWN_ADDRESSES)
    public ResponseEntity<ApiResponse<AddressDto>> create(@Valid @RequestBody UpsertAddressRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(service.create(actor.require().userId(), r), "Address created"));
    }

    @PutMapping("/{id}")
    @RequiresPermission(Permissions.MANAGE_OWN_ADDRESSES)
    public ApiResponse<AddressDto> update(@PathVariable UUID id, @Valid @RequestBody UpsertAddressRequest r) {
        return ApiResponse.ok(service.update(actor.require().userId(), id, r), "Address updated");
    }

    @PostMapping("/{id}/default")
    @RequiresPermission(Permissions.MANAGE_OWN_ADDRESSES)
    public ApiResponse<AddressDto> setDefault(@PathVariable UUID id) {
        return ApiResponse.ok(service.setDefault(actor.require().userId(), id), "Default address set");
    }

    @DeleteMapping("/{id}")
    @RequiresPermission(Permissions.MANAGE_OWN_ADDRESSES)
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(actor.require().userId(), id);
        return ApiResponse.ok(null, "Address deleted");
    }
}
