package com.commercesuite.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercesuite.AbstractIT;
import com.commercesuite.auth.dto.SignupRequest;
import com.commercesuite.auth.event.AuthEvents;
import com.commercesuite.auth.service.AuthService;
import com.commercesuite.common.outbox.OutboxEventRepository;
import com.commercesuite.rbac.entity.AppRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AuthEventPublicationIT extends AbstractIT {

    @Autowired AuthService auth;
    @Autowired OutboxEventRepository outboxRepo;

    @Test
    void signup_writes_user_registered_to_outbox() {
        String email = "evt-" + System.nanoTime() + "@example.com";
        auth.signup(new SignupRequest(email, "Password123!",
                        "Test User", "+919999000111", AppRole.CUSTOMER),
                "JUnit", "127.0.0.1");
        boolean found = outboxRepo.findAll().stream()
                .anyMatch(e -> AuthEvents.USER_REGISTERED.equals(e.getEventType())
                        && e.getPayload().contains(email));
        assertThat(found).as("outbox row for USER_REGISTERED").isTrue();
    }
}