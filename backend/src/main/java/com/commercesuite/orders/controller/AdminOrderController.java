package com.commercesuite.orders.controller;
import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.api.PageResponse;
import com.commercesuite.orders.dto.OrderDto;
import com.commercesuite.orders.service.OrderService;
import com.commercesuite.rbac.service.Permissions;
import com.commercesuite.rbac.service.RequiresPermission;
import com.commercesuite.returns.dto.ReturnRequestDto;
import com.commercesuite.returns.service.ReturnService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Orders")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@RequiresPermission(Permissions.MANAGE_USERS)
public class AdminOrderController {
  private final OrderService orderService;
  private final ReturnService returnService;

  @GetMapping("/orders")
  public ApiResponse<PageResponse<OrderDto>> listOrders(@RequestParam(defaultValue="0") int page,
                                                        @RequestParam(defaultValue="20") int size) {
    return ApiResponse.ok(PageResponse.of(orderService.listAll(
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "placedAt")))));
  }

  @GetMapping("/returns")
  public ApiResponse<PageResponse<ReturnRequestDto>> listReturns(@RequestParam(defaultValue="0") int page,
                                                                 @RequestParam(defaultValue="20") int size) {
    return ApiResponse.ok(PageResponse.of(returnService.listAll(
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestedAt")))));
  }
}
