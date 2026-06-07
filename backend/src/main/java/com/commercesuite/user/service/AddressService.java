package com.commercesuite.user.service;

import com.commercesuite.common.exception.AppException;
import com.commercesuite.user.dto.AddressDto;
import com.commercesuite.user.dto.UpsertAddressRequest;
import com.commercesuite.user.entity.Address;
import com.commercesuite.user.repository.AddressRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddressService {
    private final AddressRepository repo;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<AddressDto> list(UUID userId) {
        return repo.findByUserIdAndDeletedAtIsNullOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public AddressDto create(UUID userId, UpsertAddressRequest r) {
        if (r.isDefault()) repo.clearDefaults(userId);
        boolean first = repo.findByUserIdAndDeletedAtIsNullOrderByIsDefaultDescCreatedAtDesc(userId).isEmpty();
        Address a = Address.builder()
                .userId(userId).type(r.type()).contactName(r.contactName()).phone(r.phone())
                .line1(r.line1()).line2(r.line2()).city(r.city()).state(r.state())
                .pincode(r.pincode()).country(r.country() == null ? "IN" : r.country())
                .isDefault(r.isDefault() || first).build();
        return toDto(repo.save(a));
    }

    @Transactional
    public AddressDto update(UUID userId, UUID id, UpsertAddressRequest r) {
        Address a = repo.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .orElseThrow(() -> AppException.notFound("Address"));
        if (r.isDefault() && !a.isDefault()) repo.clearDefaults(userId);
        a.setType(r.type()); a.setContactName(r.contactName()); a.setPhone(r.phone());
        a.setLine1(r.line1()); a.setLine2(r.line2()); a.setCity(r.city());
        a.setState(r.state()); a.setPincode(r.pincode());
        if (r.country() != null) a.setCountry(r.country());
        a.setDefault(r.isDefault() || a.isDefault());
        return toDto(a);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Address a = repo.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .orElseThrow(() -> AppException.notFound("Address"));
        // Soft delete via @SQLDelete trigger on repository.delete()
        repo.delete(a);
        // Touch updatedAt explicitly is unnecessary; @SQLDelete handles the row.
        // (Instant.now(clock) reserved for future scheduled purges.)
        Instant.now(clock);
    }

    @Transactional
    public AddressDto setDefault(UUID userId, UUID id) {
        Address a = repo.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .orElseThrow(() -> AppException.notFound("Address"));
        repo.clearDefaults(userId);
        a.setDefault(true);
        return toDto(a);
    }

    private AddressDto toDto(Address a) {
        return new AddressDto(a.getId(), a.getType(), a.getContactName(), a.getPhone(),
                a.getLine1(), a.getLine2(), a.getCity(), a.getState(), a.getPincode(),
                a.getCountry(), a.isDefault());
    }
}
