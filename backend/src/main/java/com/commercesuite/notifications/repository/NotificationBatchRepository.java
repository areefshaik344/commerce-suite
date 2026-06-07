package com.commercesuite.notifications.repository;

import com.commercesuite.notifications.domain.NotificationBatch;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationBatchRepository extends JpaRepository<NotificationBatch, UUID> {}