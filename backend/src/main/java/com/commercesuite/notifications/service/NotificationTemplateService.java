package com.commercesuite.notifications.service;

import com.commercesuite.notifications.domain.NotificationTemplate;
import com.commercesuite.notifications.preferences.NotificationChannel;
import com.commercesuite.notifications.repository.NotificationTemplateRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationTemplateService {

    private final NotificationTemplateRepository repo;

    @Transactional(readOnly = true)
    public Optional<NotificationTemplate> findActive(String code, NotificationChannel channel, String locale) {
        return repo.findFirstByCodeAndChannelAndLocaleAndActiveTrueAndDeletedAtIsNullOrderByVersionDesc(
                code, channel, locale == null ? "en" : locale);
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplate> listAll() { return repo.findByActiveTrueAndDeletedAtIsNull(); }
}