package com.commercesuite.cart.service;

import com.commercesuite.cart.dto.*;
import com.commercesuite.cart.entity.*;
import com.commercesuite.cart.event.CartEvents.*;
import com.commercesuite.cart.repository.*;
import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.exception.AppException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepo;
    private final CartItemRepository itemRepo;
    private final SavedForLaterItemRepository sflRepo;
    private final CartValidationService validator;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public Cart getOrCreateActiveCart(UUID userId) {
        return cartRepo.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseGet(() -> cartRepo.save(Cart.builder()
                        .userId(userId).status(CartStatus.ACTIVE)
                        .currency("INR").lastActivityAt(Instant.now(clock))
                        .build()));
    }

    @Transactional(readOnly = true)
    public CartDto get(ActorContext actor) {
        Cart cart = getOrCreateActiveCart(actor.userId());
        var items = itemRepo.findByCartId(cart.getId()).stream().map(CartItemDto::from).toList();
        return CartDto.from(cart, items);
    }

    @Transactional
    public CartDto addItem(AddCartItemRequest req, ActorContext actor) {
        Cart cart = getOrCreateActiveCart(actor.userId());
        var validated = validator.validate(req.variantId(), req.qty());
        var existing = itemRepo.findByCartIdAndVariantId(cart.getId(), req.variantId());
        CartItem saved;
        if (existing.isPresent()) {
            CartItem item = existing.get();
            int newQty = item.getQty() + req.qty();
            validator.validate(req.variantId(), newQty);
            int oldQty = item.getQty();
            item.setQty(newQty);
            saved = itemRepo.save(item);
            events.publishEvent(new CartItemUpdatedEvent(cart.getId(), actor.userId(),
                    req.variantId(), oldQty, newQty, Instant.now(clock)));
        } else {
            saved = itemRepo.save(CartItem.builder()
                    .cartId(cart.getId())
                    .productId(validated.product().getId())
                    .variantId(req.variantId())
                    .vendorId(validated.product().getVendorId())
                    .qty(req.qty())
                    .unitPricePaise(validated.variant().getPricePaise())
                    .currency(validated.variant().getCurrency())
                    .addedAt(Instant.now(clock))
                    .build());
            events.publishEvent(new CartItemAddedEvent(cart.getId(), actor.userId(),
                    req.variantId(), req.qty(), saved.getUnitPricePaise(), Instant.now(clock)));
        }
        touch(cart);
        return reload(cart);
    }

    @Transactional
    public CartDto updateItem(UUID itemId, UpdateCartItemRequest req, ActorContext actor) {
        CartItem item = loadOwnedItem(itemId, actor);
        validator.validate(item.getVariantId(), req.qty());
        int oldQty = item.getQty();
        item.setQty(req.qty());
        itemRepo.save(item);
        events.publishEvent(new CartItemUpdatedEvent(item.getCartId(), actor.userId(),
                item.getVariantId(), oldQty, req.qty(), Instant.now(clock)));
        Cart cart = cartRepo.findById(item.getCartId()).orElseThrow();
        touch(cart);
        return reload(cart);
    }

    @Transactional
    public CartDto removeItem(UUID itemId, ActorContext actor) {
        CartItem item = loadOwnedItem(itemId, actor);
        UUID cartId = item.getCartId();
        UUID variantId = item.getVariantId();
        itemRepo.delete(item);
        events.publishEvent(new CartItemRemovedEvent(cartId, actor.userId(), variantId, Instant.now(clock)));
        Cart cart = cartRepo.findById(cartId).orElseThrow();
        touch(cart);
        return reload(cart);
    }

    @Transactional
    public SavedForLaterItemDto saveForLater(UUID cartItemId, ActorContext actor) {
        CartItem item = loadOwnedItem(cartItemId, actor);
        SavedForLaterItem sfl = sflRepo.findByUserIdAndVariantId(actor.userId(), item.getVariantId())
                .orElseGet(() -> SavedForLaterItem.builder()
                        .userId(actor.userId()).productId(item.getProductId())
                        .variantId(item.getVariantId()).qty(item.getQty())
                        .savedAt(Instant.now(clock)).build());
        sfl.setQty(item.getQty());
        SavedForLaterItem saved = sflRepo.save(sfl);
        UUID cartId = item.getCartId();
        itemRepo.delete(item);
        events.publishEvent(new SavedForLaterEvent(actor.userId(), item.getVariantId(), Instant.now(clock)));
        cartRepo.findById(cartId).ifPresent(this::touch);
        return SavedForLaterItemDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<SavedForLaterItemDto> listSaved(ActorContext actor) {
        return sflRepo.findByUserId(actor.userId()).stream().map(SavedForLaterItemDto::from).toList();
    }

    /** Guest -> user migration hook. Merges guest cart items into the user's active cart. */
    @Transactional
    public Cart mergeGuestCart(String guestToken, UUID userId) {
        var guest = cartRepo.findByGuestTokenAndStatus(guestToken, CartStatus.ACTIVE).orElse(null);
        if (guest == null) return getOrCreateActiveCart(userId);
        Cart target = getOrCreateActiveCart(userId);
        for (CartItem item : itemRepo.findByCartId(guest.getId())) {
            var existing = itemRepo.findByCartIdAndVariantId(target.getId(), item.getVariantId());
            int desired = (existing.map(CartItem::getQty).orElse(0)) + item.getQty();
            try {
                validator.validate(item.getVariantId(), desired);
                if (existing.isPresent()) {
                    existing.get().setQty(desired); itemRepo.save(existing.get());
                } else {
                    itemRepo.save(CartItem.builder()
                            .cartId(target.getId()).productId(item.getProductId())
                            .variantId(item.getVariantId()).vendorId(item.getVendorId())
                            .qty(item.getQty()).unitPricePaise(item.getUnitPricePaise())
                            .currency(item.getCurrency()).addedAt(Instant.now(clock)).build());
                }
            } catch (AppException ignored) { /* skip invalid lines during merge */ }
        }
        guest.setStatus(CartStatus.MERGED);
        guest.setMergedIntoId(target.getId());
        cartRepo.save(guest);
        events.publishEvent(new CartMergedEvent(guest.getId(), target.getId(), userId, Instant.now(clock)));
        touch(target);
        return target;
    }

    @Transactional
    public void markConverted(UUID cartId) {
        cartRepo.findById(cartId).ifPresent(c -> {
            c.setStatus(CartStatus.CONVERTED);
            cartRepo.save(c);
        });
    }

    /* ---- helpers ---- */

    private CartItem loadOwnedItem(UUID itemId, ActorContext actor) {
        CartItem item = itemRepo.findById(itemId).orElseThrow(() -> AppException.notFound("CartItem"));
        Cart cart = cartRepo.findById(item.getCartId()).orElseThrow(() -> AppException.notFound("Cart"));
        if (cart.getUserId() == null || !cart.getUserId().equals(actor.userId()))
            throw AppException.forbidden("Not your cart");
        return item;
    }

    private void touch(Cart cart) {
        cart.setLastActivityAt(Instant.now(clock));
        cartRepo.save(cart);
    }

    private CartDto reload(Cart cart) {
        var items = itemRepo.findByCartId(cart.getId()).stream().map(CartItemDto::from).toList();
        return CartDto.from(cart, items);
    }
}