package main.service;

import main.model.Event;
import main.model.Subscription;
import main.model.User;
import main.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final TicketService ticketService;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               TicketService ticketService) {
        this.subscriptionRepository = subscriptionRepository;
        this.ticketService = ticketService;
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

    public boolean existsByUserAndEvent(UUID userId, UUID eventId) {
        return subscriptionRepository.existsByUserIdAndEventId(userId, eventId);
    }

    public long countByEvent(UUID eventId) {
        return subscriptionRepository.countByEventId(eventId);
    }

    public List<Subscription> findByEventId(UUID eventId) {
        return subscriptionRepository.findByEventId(eventId);
    }

    @Transactional
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
        ticketService.issueTicket(saved);
        return saved;
    }

    public void deleteByUserAndEvent(UUID userId, UUID eventId) {
        Optional<Subscription> subscription = subscriptionRepository.findByUserIdAndEventId(userId, eventId);
        subscription.ifPresent(value -> {
            ticketService.deleteBySubscriptionId(value.getId());
            subscriptionRepository.delete(value);
        });
    }

    @Transactional
    public void deleteAllByEventId(UUID eventId) {
        subscriptionRepository.findByEventId(eventId).forEach(subscription -> {
            ticketService.deleteBySubscriptionId(subscription.getId());
            subscriptionRepository.delete(subscription);
        });
    }

    @Transactional
    public void deleteAllByUserId(UUID userId) {
        subscriptionRepository.findByUserId(userId).forEach(subscription -> {
            ticketService.deleteBySubscriptionId(subscription.getId());
            subscriptionRepository.delete(subscription);
        });
    }
}
