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
    public void issueTicket(Subscription subscription) {
        String code = UUID.randomUUID().toString();
        Ticket ticket = Ticket.builder()
                .subscription(subscription)
                .code(code)
                .issuedAt(LocalDateTime.now())
                .build();
        ticketRepository.save(ticket);
        logger.info("Ticket issued for subscription {} with code {}", subscription.getId(), ticket.getCode());
    }

    public Map<UUID, Ticket> getTicketsForUser(UUID userId) {
        List<Ticket> tickets = ticketRepository.findByUserId(userId);
        return tickets.stream()
                .collect(Collectors.toMap(ticket -> ticket.getSubscription().getEvent().getId(), ticket -> ticket));
    }



    public Optional<Ticket> findWithDetailsByCode(String code) {
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

