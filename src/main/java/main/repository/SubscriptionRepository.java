package main.repository;

import main.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findByUserId(UUID userId);

    Optional<Subscription> findByUserIdAndEventId(UUID userId, UUID eventId);

    boolean existsByUserIdAndEventId(UUID userId, UUID eventId);

    long countByEventId(UUID eventId);

    List<Subscription> findByEventId(UUID eventId);
}
