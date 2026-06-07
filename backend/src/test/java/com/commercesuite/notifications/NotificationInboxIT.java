package com.commercesuite.notifications;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercesuite.AbstractIT;
import com.commercesuite.notifications.preferences.NotificationCategory;
import com.commercesuite.notifications.preferences.NotificationChannel;
import com.commercesuite.notifications.service.NotificationInboxService;
import com.commercesuite.notifications.service.NotificationService;
import com.commercesuite.notifications.service.NotificationService.CreateRequest;
import com.commercesuite.user.entity.User;
import com.commercesuite.user.repository.UserRepository;
import java.util.EnumSet;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

class NotificationInboxIT extends AbstractIT {

    @Autowired NotificationService notifications;
    @Autowired NotificationInboxService inbox;
    @Autowired UserRepository userRepo;
    @Autowired PasswordEncoder encoder;

    @Test @Transactional
    void inbox_lifecycle_create_count_mark_read() {
        User u = userRepo.save(User.builder()
                .email("inbox-" + System.nanoTime() + "@example.com")
                .passwordHash(encoder.encode("Password123!"))
                .build());

        var n = notifications.createAndDispatch(new CreateRequest(
                u.getId(), "auth.user.registered", NotificationCategory.AUTH,
                EnumSet.of(NotificationChannel.IN_APP),
                Map.of("appName", "CommerceSuite", "name", "Test"),
                null, "auth.user.registered", null, null));

        assertThat(inbox.unreadCount(u.getId())).isEqualTo(1L);
        var read = inbox.markRead(u.getId(), n.getId());
        assertThat(read.getReadAt()).isNotNull();
        assertThat(inbox.unreadCount(u.getId())).isZero();
    }
}