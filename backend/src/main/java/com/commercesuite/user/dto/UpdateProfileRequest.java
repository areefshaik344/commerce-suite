package com.commercesuite.user.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateProfileRequest(
    @Size(min = 2, max = 120) String fullName,
    @Size(max = 80) String displayName,
    @Size(max = 500) String avatarUrl,
    @Pattern(regexp = "male|female|other|prefer_not_to_say") String gender,
    @Past LocalDate dateOfBirth,
    @Size(max = 280) String bio,
    @Pattern(regexp = "[a-z]{2}-[A-Z]{2}") String locale
) {}
