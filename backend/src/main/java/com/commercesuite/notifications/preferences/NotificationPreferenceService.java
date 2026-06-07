package com.commercesuite.notifications.preferences;

import com.commercesuite.notifications.preferences.dto.PreferenceEntryDto;
import com.commercesuite.notifications.preferences.dto.UpdatePreferencesRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository repo;

    @Transactional(readOnly = true)
    public List<PreferenceEntryDto> listFor(UUID userId) {
        List<NotificationPreference> rows = repo.findByUserId(userId);
        // ensure full matrix returned (defaults for missing combos)
        List<PreferenceEntryDto> out = new ArrayList<>();
        for (NotificationChannel ch : NotificationChannel.values()) {
            for (NotificationCategory cat : NotificationCategory.values()) {
                var existing = rows.stream()
                        .filter(r -> r.getChannel() == ch && r.getCategory() == cat)
                        .findFirst();
                if (existing.isPresent()) {
                    var p = existing.get();
                    out.add(new PreferenceEntryDto(ch, cat, p.isEnabled(), p.isMarketingOptIn()));
                } else {
                    out.add(new PreferenceEntryDto(ch, cat, defaultEnabled(ch, cat), false));
                }
            }
        }
        return out;
    }

    @Transactional
    public List<PreferenceEntryDto> upsert(UUID userId, UpdatePreferencesRequest req) {
        for (PreferenceEntryDto e : req.entries()) {
            var existing = repo.findByUserIdAndChannelAndCategory(userId, e.channel(), e.category());
            if (existing.isPresent()) {
                var p = existing.get();
                p.setEnabled(e.enabled());
                p.setMarketingOptIn(e.marketingOptIn());
            } else {
                repo.save(NotificationPreference.builder()
                        .userId(userId)
                        .channel(e.channel())
                        .category(e.category())
                        .enabled(e.enabled())
                        .marketingOptIn(e.marketingOptIn())
                        .build());
            }
        }
        return listFor(userId);
    }

    /** Default opt-in: AUTH/ORDER/PAYMENT/REFUND/SYSTEM on every channel; VENDOR only on IN_APP/EMAIL. */
    private static boolean defaultEnabled(NotificationChannel ch, NotificationCategory cat) {
        if (cat == NotificationCategory.VENDOR && (ch == NotificationChannel.SMS || ch == NotificationChannel.PUSH))
            return false;
        return true;
    }
}