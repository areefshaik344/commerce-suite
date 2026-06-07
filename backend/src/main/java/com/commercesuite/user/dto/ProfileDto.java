package com.commercesuite.user.dto;

import com.commercesuite.user.entity.AccountStatus;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record ProfileDto(
    UUID id,
    String email,
    String phone,
    boolean emailVerified,
    boolean phoneVerified,
    AccountStatus accountStatus,
    Set<String> roles,
    String fullName,
    String displayName,
    String avatarUrl,
    String gender,
    LocalDate dateOfBirth,
    String bio,
    String locale
) {}
