package com.commercesuite.user.repository;

import com.commercesuite.user.entity.Address;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AddressRepository extends JpaRepository<Address, UUID> {
    List<Address> findByUserIdAndDeletedAtIsNullOrderByIsDefaultDescCreatedAtDesc(UUID userId);
    Optional<Address> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    @Modifying
    @Query("update Address a set a.isDefault = false where a.userId = :uid and a.deletedAt is null")
    void clearDefaults(@Param("uid") UUID userId);
}
