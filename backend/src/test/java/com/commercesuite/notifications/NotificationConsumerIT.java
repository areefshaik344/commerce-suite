package com.commercesuite.notifications;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercesuite.AbstractIT;
import com.commercesuite.auth.dto.SignupRequest;
import com.commercesuite.auth.service.AuthService;
import com.commercesuite.common.outbox.OutboxDispatcher;
import com.commercesuite.notifications.repository.NotificationRepository;
import com.commercesuite.rbac.entity.AppRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotificationConsumerIT extends AbstractIT {

    @Autowired AuthService auth;
    @Autowired OutboxDispatcher dispatcher;
    @Autowired NotificationRepository notifRepo;

    @Test
    void signup_event_produces_in_app_notification_via_consumer() {
        var r = auth.signup(new SignupRequest(
                "consumer-" + System.nanoTime() + "@example.com",
                "Password123!", "Tester", "+919999000222", AppRole.CUSTOMER),
                "JUnit", "127.0.0.1");
        // force a dispatch tick so consumer fires
        dispatcher.dispatchBatch();
        long count = notifRepo.listForUser(r.userId(),
                org.springframework.data.domain.PageRequest.of(0, 50)).size();
        assertThat(count).isGreaterThanOrEqualTo(1);
    }
}