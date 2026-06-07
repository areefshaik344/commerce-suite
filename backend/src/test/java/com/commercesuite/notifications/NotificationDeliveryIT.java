package com.commercesuite.notifications;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercesuite.AbstractIT;
import com.commercesuite.notifications.domain.NotificationStatus;
import com.commercesuite.notifications.preferences.NotificationCategory;
import com.commercesuite.notifications.preferences.NotificationChannel;
import com.commercesuite.notifications.repository.NotificationDeliveryRepository;
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

class NotificationDeliveryIT extends AbstractIT {

    @Autowired NotificationService notifications;
    @Autowired NotificationDeliveryRepository deliveryRepo;
    @Autowired UserRepository userRepo;
    @Autowired PasswordEncoder encoder;

    @Test @Transactional
    void in_app_delivery_marks_delivered_synchronously() {
        User u = userRepo.save(User.builder()
                .email("deliv-" + System.nanoTime() + "@example.com")
                .passwordHash(encoder.encode("Password123!"))
                .build());

        var n = notifications.createAndDispatch(new CreateRequest(
                u.getId(), "order.created", NotificationCategory.ORDER,
                EnumSet.of(NotificationChannel.IN_APP),
                Map.of("orderNumber", "ORD-1", "total", "₹100"),
                null, "order.created", null, null));

        var ds = deliveryRepo.findByNotificationId(n.getId());
        assertThat(ds).hasSize(1);
        assertThat(ds.get(0).getStatus()).isEqualTo(NotificationStatus.DELIVERED);
        assertThat(ds.get(0).getSentAt()).isNotNull();
    }
}