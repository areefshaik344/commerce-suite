package com.commercesuite.shipping.service;
import com.commercesuite.shipping.dto.AddTrackingEventRequest;
import com.commercesuite.shipping.dto.TrackingEventDto;
import com.commercesuite.shipping.entity.TrackingEvent;
import com.commercesuite.shipping.event.ShippingEvents.TrackingEventRecordedEvent;
import com.commercesuite.shipping.repository.TrackingEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrackingService {
    private final TrackingEventRepository repo;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public TrackingEventDto add(UUID shipmentId, AddTrackingEventRequest req) {
        TrackingEvent e = repo.save(TrackingEvent.builder()
                .shipmentId(shipmentId)
                .eventType(req.eventType())
                .description(req.description())
                .location(req.location())
                .occurredAt(Instant.now(clock))
                .build());
        events.publishEvent(new TrackingEventRecordedEvent(e.getId(), shipmentId, e.getEventType(), e.getOccurredAt()));
        return TrackingEventDto.from(e);
    }

    @Transactional(readOnly = true)
    public List<TrackingEventDto> listForShipment(UUID shipmentId) {
        return repo.findByShipmentIdOrderByOccurredAtAsc(shipmentId).stream().map(TrackingEventDto::from).toList();
    }
}
