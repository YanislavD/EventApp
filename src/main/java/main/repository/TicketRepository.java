package main.repository;

import main.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    @Query("select t from Ticket t join fetch t.subscription s join fetch s.event e where s.user.id = :userId")
    List<Ticket> findByUserId(@Param("userId") UUID userId);

    @Query("select t from Ticket t " +
            "join fetch t.subscription s " +
            "join fetch s.user u " +
            "join fetch s.event e " +
            "where t.code = :code")
    Optional<Ticket> findWithDetailsByCode(@Param("code") String code);

    Optional<Ticket> findBySubscriptionId(UUID subscriptionId);

    void deleteBySubscriptionId(UUID subscriptionId);
}

