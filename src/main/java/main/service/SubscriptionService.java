package main.service;

import main.model.Event;
import main.model.Subscription;
import main.model.User;
import main.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public List<Subscription> findByUserId(UUID id) {
        return subscriptionRepository.findByUserId(id);
    }

    public Set<UUID> getSubscribedEventIds(UUID userId) {
        return subscriptionRepository.findByUserId(userId).stream()
                .map(Subscription::getEvent)
                .filter(Objects::nonNull)
                .map(Event::getId)
                .collect(Collectors.toSet());
    }

    public List<Event> getSubscribedEvents(UUID userId) {
        return subscriptionRepository.findByUserId(userId).stream()
                .map(Subscription::getEvent)
                .filter(Objects::nonNull)
                .toList();
    }

    public boolean existsByUserAndEvent(UUID userId, UUID eventId) {
        return subscriptionRepository.existsByUserIdAndEventId(userId, eventId);
    }

    public long countByEvent(UUID eventId) {
        return subscriptionRepository.countByEventId(eventId);
    }

    @SuppressWarnings("null")
    public Subscription create(User user, Event event) {
        if (user == null) {
            throw new IllegalArgumentException("Потребителят е задължителен");
        }
        if (event == null) {
            throw new IllegalArgumentException("Събитието е задължително");
        }
        Subscription subscription = Subscription.builder()
                .user(user)
                .event(event)
                .subscriptionTime(LocalDateTime.now())
                .build();
        Subscription saved = subscriptionRepository.save(subscription);
        return Objects.requireNonNull(saved, "Регистрацията не беше запазена");
    }
}
