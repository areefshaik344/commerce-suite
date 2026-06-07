package com.commercesuite.cart.controller;

import com.commercesuite.cart.dto.*;
import com.commercesuite.cart.service.CartService;
import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.audit.ActorContextHolder;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Cart")
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final ActorContextHolder actor;

    @GetMapping
    public ApiResponse<CartDto> get() {
        return ApiResponse.ok(cartService.get(actor.require()));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartDto>> add(@Valid @RequestBody AddCartItemRequest req) {
        var out = cartService.addItem(req, actor.require());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(out, "Item added"));
    }

    @PutMapping("/items/{id}")
    public ApiResponse<CartDto> update(@PathVariable UUID id, @Valid @RequestBody UpdateCartItemRequest req) {
        return ApiResponse.ok(cartService.updateItem(id, req, actor.require()), "Item updated");
    }

    @DeleteMapping("/items/{id}")
    public ApiResponse<CartDto> remove(@PathVariable UUID id) {
        return ApiResponse.ok(cartService.removeItem(id, actor.require()), "Item removed");
    }

    @PostMapping("/save-for-later")
    public ApiResponse<SavedForLaterItemDto> saveForLater(@Valid @RequestBody SaveForLaterRequest req) {
        return ApiResponse.ok(cartService.saveForLater(req.cartItemId(), actor.require()), "Saved");
    }

    @GetMapping("/save-for-later")
    public ApiResponse<List<SavedForLaterItemDto>> listSaved() {
        return ApiResponse.ok(cartService.listSaved(actor.require()));
    }
}