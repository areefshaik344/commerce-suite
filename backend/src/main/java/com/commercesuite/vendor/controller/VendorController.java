package com.commercesuite.vendor.controller;

import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.audit.ActorContextHolder;
import com.commercesuite.rbac.service.Permissions;
import com.commercesuite.rbac.service.RequiresPermission;
import com.commercesuite.vendor.dto.*;
import com.commercesuite.vendor.service.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Vendor (self)")
@RestController
@RequestMapping("/api/v1/vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;
    private final VendorApplicationService applicationService;
    private final VendorBankService bankService;
    private final ActorContextHolder actor;

    /** Customer applies to become a vendor. */
    @PostMapping("/apply")
    @RequiresPermission(Permissions.APPLY_AS_VENDOR)
    public ResponseEntity<ApiResponse<VendorApplicationDto>> apply(@Valid @RequestBody ApplyVendorRequest r) {
        var out = applicationService.apply(actor.require().userId(), r);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(out, "Application submitted"));
    }

    @GetMapping("/me")
    @RequiresPermission(Permissions.MANAGE_VENDOR_PROFILE)
    public ApiResponse<VendorDto> me() { return ApiResponse.ok(vendorService.me(actor.require().userId())); }

    @GetMapping("/me/profile")
    @RequiresPermission(Permissions.MANAGE_VENDOR_PROFILE)
    public ApiResponse<VendorProfileDto> profile() {
        return ApiResponse.ok(vendorService.myProfile(actor.require().userId()));
    }

    @PutMapping("/me")
    @RequiresPermission(Permissions.MANAGE_VENDOR_PROFILE)
    public ApiResponse<VendorProfileDto> updateProfile(@Valid @RequestBody UpdateVendorProfileRequest r) {
        return ApiResponse.ok(vendorService.updateProfile(actor.require().userId(), r), "Profile updated");
    }

    @GetMapping("/me/verification")
    @RequiresPermission(Permissions.MANAGE_VENDOR_PROFILE)
    public ApiResponse<VendorVerificationDto> verification() {
        return ApiResponse.ok(vendorService.myVerification(actor.require().userId()));
    }

    @PostMapping("/me/documents")
    @RequiresPermission(Permissions.MANAGE_VENDOR_PROFILE)
    public ResponseEntity<ApiResponse<VendorDocumentDto>> uploadDocument(@Valid @RequestBody UpsertDocumentRequest r) {
        var dto = vendorService.uploadDocument(actor.require().userId(), r);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(dto, "Document submitted"));
    }

    @GetMapping("/me/documents")
    @RequiresPermission(Permissions.MANAGE_VENDOR_PROFILE)
    public ApiResponse<List<VendorDocumentDto>> listDocuments() {
        return ApiResponse.ok(vendorService.listDocuments(actor.require().userId()));
    }

    @PostMapping("/me/bank-account")
    @RequiresPermission(Permissions.MANAGE_VENDOR_PROFILE)
    public ResponseEntity<ApiResponse<VendorBankAccountDto>> upsertBank(@Valid @RequestBody UpsertBankAccountRequest r) {
        var dto = bankService.upsertPrimary(actor.require().userId(), r);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(dto, "Bank account saved"));
    }

    @GetMapping("/me/bank-accounts")
    @RequiresPermission(Permissions.MANAGE_VENDOR_PROFILE)
    public ApiResponse<List<VendorBankAccountDto>> listBank() {
        return ApiResponse.ok(vendorService.listBankAccounts(actor.require().userId()));
    }

    @GetMapping("/me/applications")
    @RequiresPermission(Permissions.APPLY_AS_VENDOR)
    public ApiResponse<List<VendorApplicationDto>> myApplications() {
        return ApiResponse.ok(applicationService.listForUser(actor.require().userId()));
    }
}