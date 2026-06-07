package com.commercesuite.orders.controller;
import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.api.PageResponse;
import com.commercesuite.common.audit.ActorContextHolder;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.orders.dto.OrderItemDto;
import com.commercesuite.orders.dto.VendorOrderDto;
import com.commercesuite.orders.entity.VendorOrder;
import com.commercesuite.orders.entity.VendorOrderStatus;
import com.commercesuite.orders.repository.OrderItemRepository;
import com.commercesuite.orders.repository.OrderRepository;
import com.commercesuite.orders.repository.VendorOrderRepository;
import com.commercesuite.orders.service.OrderRollupService;
import com.commercesuite.orders.service.VendorOrderOwnershipGuard;
import com.commercesuite.orders.service.VendorOrderStateMachine;
import com.commercesuite.rbac.service.Permissions;
import com.commercesuite.rbac.service.RequiresPermission;
import com.commercesuite.returns.service.ReturnService;
import com.commercesuite.vendor.entity.Vendor;
import com.commercesuite.vendor.repository.VendorRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Vendor Orders")
@RestController
@RequestMapping("/api/v1/vendor/orders")
@RequiredArgsConstructor
@RequiresPermission(Permissions.MANAGE_VENDOR_ORDERS)
public class VendorOrderController {
  private final VendorOrderRepository repo;
  private final OrderItemRepository itemRepo;
  private final OrderRepository orderRepo;
  private final VendorOrderStateMachine fsm;
  private final VendorOrderOwnershipGuard ownership;
  private final OrderRollupService rollup;
  private final VendorRepository vendorRepo;
  private final ReturnService returnService;
  private final ActorContextHolder actor;

  @GetMapping
  public ApiResponse<PageResponse<VendorOrderDto>> mine(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
    Vendor v = vendorRepo.findByUserId(actor.require().userId())
        .orElseThrow(() -> AppException.forbidden("Not a vendor"));
    var p = repo.findByVendorId(v.getId(),
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    return ApiResponse.ok(PageResponse.of(p.map(this::dto)));
  }

  @GetMapping("/{id}")
  public ApiResponse<VendorOrderDto> get(@PathVariable UUID id) {
    VendorOrder vo = load(id);
    return ApiResponse.ok(dto(vo));
  }

  @PostMapping("/{id}/accept")
  public ApiResponse<VendorOrderDto> accept(@PathVariable UUID id) {
    VendorOrder vo = load(id);
    fsm.transition(vo, VendorOrderStatus.CONFIRMED, actor.require().userId(), "vendor", "accept");
    repo.save(vo);
    return ApiResponse.ok(dto(vo), "Accepted");
  }

  @PostMapping("/{id}/process")
  public ApiResponse<VendorOrderDto> process(@PathVariable UUID id) {
    VendorOrder vo = load(id);
    fsm.transition(vo, VendorOrderStatus.PROCESSING, actor.require().userId(), "vendor", "processing");
    repo.save(vo);
    return ApiResponse.ok(dto(vo), "Processing");
  }

  @PostMapping("/{id}/ship")
  public ApiResponse<VendorOrderDto> ship(@PathVariable UUID id) {
    VendorOrder vo = load(id);
    fsm.transition(vo, VendorOrderStatus.SHIPPED, actor.require().userId(), "vendor", "ship");
    repo.save(vo);
    return ApiResponse.ok(dto(vo), "Shipped");
  }

  @PostMapping("/{id}/deliver")
  public ApiResponse<VendorOrderDto> deliver(@PathVariable UUID id) {
    VendorOrder vo = load(id);
    fsm.transition(vo, VendorOrderStatus.DELIVERED, actor.require().userId(), "vendor", "deliver");
    repo.save(vo);
    var o = orderRepo.findById(vo.getOrderId()).orElseThrow();
    rollup.rollup(o); orderRepo.save(o);
    return ApiResponse.ok(dto(vo), "Delivered");
  }

  @PostMapping("/{id}/returns/{returnId}/approve")
  public ApiResponse<?> approveReturn(@PathVariable UUID id, @PathVariable UUID returnId) {
    load(id); // ownership
    return ApiResponse.ok(returnService.approve(returnId, actor.require()), "Approved");
  }

  private VendorOrder load(UUID id) {
    VendorOrder vo = repo.findById(id).orElseThrow(() -> AppException.notFound("VendorOrder"));
    ownership.requireVendorOrAdmin(vo, actor.require());
    return vo;
  }

  private VendorOrderDto dto(VendorOrder vo) {
    var items = itemRepo.findByVendorOrderId(vo.getId()).stream().map(OrderItemDto::from).toList();
    return VendorOrderDto.from(vo, items);
  }
}
