package main.service;

import main.model.Role;
import main.model.Subscription;
import main.model.Ticket;
import main.model.User;
import main.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TicketService {

    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    @SuppressWarnings("null")
    public Ticket issueTicket(Subscription subscription) {
        Objects.requireNonNull(subscription, "Subscription is required for ticket issuing");
        String code = UUID.randomUUID().toString();
        Ticket ticket = Ticket.builder()
                .subscription(subscription)
                .code(code)
                .issuedAt(LocalDateTime.now())
                .build();
        Ticket saved = Objects.requireNonNull(ticketRepository.save(ticket), "Ticket was not persisted");
        logger.info("Ticket issued for subscription {} with code {}", subscription.getId(), saved.getCode());
        return saved;
    }

    public Map<UUID, Ticket> getTicketsForUser(UUID userId) {
        List<Ticket> tickets = ticketRepository.findByUserId(userId);
        return tickets.stream()
                .collect(Collectors.toMap(ticket -> ticket.getSubscription().getEvent().getId(), ticket -> ticket));
    }

    public Optional<Ticket> findBySubscriptionId(UUID subscriptionId) {
        return ticketRepository.findBySubscriptionId(subscriptionId);
    }

    @Transactional(readOnly = true)
    public Optional<Ticket> findWithDetailsByCode(String code) {
        Objects.requireNonNull(code, "Ticket code is required");
        return ticketRepository.findWithDetailsByCode(code);
    }

    public Ticket getTicketForQr(String code, User requester) {
        Ticket ticket = findWithDetailsByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Билетът не беше намерен"));

        boolean isOwner = ticket.getSubscription() != null
                && ticket.getSubscription().getUser() != null
                && ticket.getSubscription().getUser().getId().equals(requester.getId());
        boolean isAdmin = requester.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new IllegalStateException("Нямаш право да виждаш този билет");
        }

        return ticket;
    }

    @Transactional
    public void deleteBySubscriptionId(UUID subscriptionId) {
        ticketRepository.deleteBySubscriptionId(subscriptionId);
    }
}

