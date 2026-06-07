package com.commercesuite.notifications.preferences;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercesuite.AbstractIT;
import com.commercesuite.notifications.preferences.dto.PreferenceEntryDto;
import com.commercesuite.notifications.preferences.dto.UpdatePreferencesRequest;
import com.commercesuite.user.entity.User;
import com.commercesuite.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

class NotificationPreferenceIT extends AbstractIT {

    @Autowired NotificationPreferenceService svc;
    @Autowired UserRepository userRepo;
    @Autowired PasswordEncoder encoder;

    @Test @Transactional
    void list_returns_full_matrix_then_upsert_persists_overrides() {
        User u = userRepo.save(User.builder()
                .email("notifpref-" + System.nanoTime() + "@example.com")
                .passwordHash(encoder.encode("Password123!"))
                .build());

        List<PreferenceEntryDto> defaults = svc.listFor(u.getId());
        int expected = NotificationChannel.values().length * NotificationCategory.values().length;
        assertThat(defaults).hasSize(expected);

        svc.upsert(u.getId(), new UpdatePreferencesRequest(List.of(
                new PreferenceEntryDto(NotificationChannel.EMAIL,
                        NotificationCategory.ORDER, false, false))));
        var after = svc.listFor(u.getId()).stream()
                .filter(p -> p.channel() == NotificationChannel.EMAIL
                        && p.category() == NotificationCategory.ORDER)
                .findFirst().orElseThrow();
        assertThat(after.enabled()).isFalse();
    }
}