package com.commercesuite.notifications;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercesuite.AbstractIT;
import com.commercesuite.notifications.domain.NotificationStatus;
import com.commercesuite.notifications.preferences.NotificationCategory;
import com.commercesuite.notifications.preferences.NotificationChannel;
import com.commercesuite.notifications.preferences.NotificationPreferenceService;
import com.commercesuite.notifications.preferences.dto.PreferenceEntryDto;
import com.commercesuite.notifications.preferences.dto.UpdatePreferencesRequest;
import com.commercesuite.notifications.service.NotificationService;
import com.commercesuite.notifications.service.NotificationService.CreateRequest;
import com.commercesuite.user.entity.User;
import com.commercesuite.user.repository.UserRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

class NotificationPreferenceSuppressionIT extends AbstractIT {

    @Autowired NotificationService notifications;
    @Autowired NotificationPreferenceService prefs;
    @Autowired UserRepository userRepo;
    @Autowired PasswordEncoder encoder;

    @Test @Transactional
    void promo_like_category_suppressed_when_all_channels_disabled() {
        User u = userRepo.save(User.builder()
                .email("supp-" + System.nanoTime() + "@example.com")
                .passwordHash(encoder.encode("Password123!"))
                .build());

        // disable VENDOR/IN_APP — only IN_APP requested → no allowed channels → SUPPRESSED
        prefs.upsert(u.getId(), new UpdatePreferencesRequest(List.of(
                new PreferenceEntryDto(NotificationChannel.IN_APP, NotificationCategory.VENDOR, false, false))));

        var n = notifications.createAndDispatch(new CreateRequest(
                u.getId(), "vendor.approved", NotificationCategory.VENDOR,
                EnumSet.of(NotificationChannel.IN_APP),
                Map.of(), null, "vendor.approved", null, null));
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.SUPPRESSED);
    }
}