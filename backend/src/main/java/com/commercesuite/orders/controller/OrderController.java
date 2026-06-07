package com.commercesuite.orders.controller;
import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.api.PageResponse;
import com.commercesuite.common.audit.ActorContextHolder;
import com.commercesuite.orders.dto.*;
import com.commercesuite.orders.service.*;
import com.commercesuite.rbac.service.Permissions;
import com.commercesuite.rbac.service.RequiresPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Orders")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
  private final OrderCreationService creation;
  private final OrderService orderService;
  private final ActorContextHolder actor;

  @PostMapping
  @RequiresPermission(Permissions.PLACE_ORDER)
  public ResponseEntity<ApiResponse<OrderDto>> create(@Valid @RequestBody CreateOrderRequest req) {
    var order = creation.create(req.checkoutId(), actor.require());
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(orderService.toDto(order), "Order created"));
  }

  @GetMapping
  public ApiResponse<PageResponse<OrderDto>> mine(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
    var p = orderService.listForCustomer(actor.require().userId(),
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "placedAt")));
    return ApiResponse.ok(PageResponse.of(p));
  }

  @GetMapping("/{id}")
  public ApiResponse<OrderDto> get(@PathVariable UUID id) {
    return ApiResponse.ok(orderService.get(id, actor.require()));
  }

  @PostMapping("/{id}/cancel")
  public ApiResponse<OrderDto> cancel(@PathVariable UUID id,
                                      @RequestBody(required = false) CancelOrderRequest req) {
    return ApiResponse.ok(orderService.cancel(id, req, actor.require()), "Cancelled");
  }
}
