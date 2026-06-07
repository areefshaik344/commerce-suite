package com.commercesuite.user.service;

import com.commercesuite.common.exception.AppException;
import com.commercesuite.rbac.service.RoleService;
import com.commercesuite.user.dto.ProfileDto;
import com.commercesuite.user.dto.UpdateProfileRequest;
import com.commercesuite.user.entity.Profile;
import com.commercesuite.user.entity.User;
import com.commercesuite.user.repository.ProfileRepository;
import com.commercesuite.user.repository.UserRepository;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final UserRepository userRepo;
    private final ProfileRepository profileRepo;
    private final RoleService roleService;

    @Transactional(readOnly = true)
    public ProfileDto getMe(UUID userId) {
        User u = userRepo.findById(userId).orElseThrow(() -> AppException.notFound("User"));
        Profile p = profileRepo.findById(userId).orElseGet(() ->
                profileRepo.save(Profile.builder().userId(userId).build()));
        var roles = roleService.rolesOf(userId).stream().map(Enum::name).collect(Collectors.toSet());
        return new ProfileDto(u.getId(), u.getEmail(), u.getPhone(),
                u.getEmailVerifiedAt() != null, u.getPhoneVerifiedAt() != null,
                u.getAccountStatus(), roles,
                p.getFullName(), p.getDisplayName(), p.getAvatarUrl(),
                p.getGender(), p.getDateOfBirth(), p.getBio(), p.getLocale());
    }

    @Transactional
    public ProfileDto updateMe(UUID userId, UpdateProfileRequest req) {
        Profile p = profileRepo.findById(userId).orElseGet(() ->
                profileRepo.save(Profile.builder().userId(userId).build()));
        if (req.fullName()     != null) p.setFullName(req.fullName());
        if (req.displayName()  != null) p.setDisplayName(req.displayName());
        if (req.avatarUrl()    != null) p.setAvatarUrl(req.avatarUrl());
        if (req.gender()       != null) p.setGender(req.gender());
        if (req.dateOfBirth()  != null) p.setDateOfBirth(req.dateOfBirth());
        if (req.bio()          != null) p.setBio(req.bio());
        if (req.locale()       != null) p.setLocale(req.locale());
        return getMe(userId);
    }
}
