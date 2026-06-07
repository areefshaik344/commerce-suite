package com.commercesuite.shipping.controller;
import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.audit.ActorContextHolder;
import com.commercesuite.rbac.service.Permissions;
import com.commercesuite.rbac.service.RequiresPermission;
import com.commercesuite.shipping.dto.*;
import com.commercesuite.shipping.service.ShipmentService;
import com.commercesuite.shipping.service.TrackingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Shipments")
@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
public class ShipmentController {
  private final ShipmentService service;
  private final TrackingService tracking;
  private final ActorContextHolder actor;

  @PostMapping
  @RequiresPermission(Permissions.MANAGE_VENDOR_ORDERS)
  public ResponseEntity<ApiResponse<ShipmentDto>> create(@Valid @RequestBody CreateShipmentRequest req) {
    var out = service.create(req, actor.require());
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(out, "Shipment created"));
  }

  @PostMapping("/{id}/status")
  @RequiresPermission(Permissions.MANAGE_VENDOR_ORDERS)
  public ApiResponse<ShipmentDto> updateStatus(@PathVariable UUID id,
                                               @Valid @RequestBody UpdateShipmentStatusRequest req) {
    return ApiResponse.ok(service.transition(id, req.status(), actor.require()), "Updated");
  }

  @GetMapping("/{id}")
  public ApiResponse<ShipmentDto> get(@PathVariable UUID id) {
    return ApiResponse.ok(service.get(id));
  }

  @PostMapping("/{id}/tracking-events")
  @RequiresPermission(Permissions.MANAGE_VENDOR_ORDERS)
  public ApiResponse<TrackingEventDto> addEvent(@PathVariable UUID id,
                                                @Valid @RequestBody AddTrackingEventRequest req) {
    return ApiResponse.ok(tracking.add(id, req), "Tracking event recorded");
  }

  @GetMapping("/{id}/tracking-events")
  public ApiResponse<List<TrackingEventDto>> listEvents(@PathVariable UUID id) {
    return ApiResponse.ok(tracking.listForShipment(id));
  }
}
