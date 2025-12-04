package main.service;

import main.model.Role;
import main.model.Subscription;
import main.model.Ticket;
import main.model.User;
import main.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private TicketService ticketService;

    private Ticket testTicket;
    private Subscription testSubscription;
    private User testOwner;

    @BeforeEach
    void setUp() {
        testOwner = new User();
        testOwner.setId(UUID.randomUUID());
        testOwner.setRole(Role.USER);

        testSubscription = new Subscription();
        testSubscription.setUser(testOwner);

        testTicket = Ticket.builder()
                .id(UUID.randomUUID())
                .code("test-code")
                .subscription(testSubscription)
                .issuedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void whenFindWithDetailsByCodeWithExistingCode_thenTicketIsReturned() {
        String code = "test-code-123";
        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .code(code)
                .issuedAt(LocalDateTime.now())
                .build();

        when(ticketRepository.findWithDetailsByCode(code)).thenReturn(Optional.of(ticket));

        Optional<Ticket> result = ticketService.findWithDetailsByCode(code);

        assertTrue(result.isPresent());
        assertEquals(code, result.get().getCode());
        verify(ticketRepository).findWithDetailsByCode(code);
    }

    @Test
    void whenFindWithDetailsByCodeWithNonExistingCode_thenEmptyIsReturned() {
        String code = "non-existing-code";

        when(ticketRepository.findWithDetailsByCode(code)).thenReturn(Optional.empty());

        Optional<Ticket> result = ticketService.findWithDetailsByCode(code);

        assertFalse(result.isPresent());
        verify(ticketRepository).findWithDetailsByCode(code);
    }

    @Test
    void whenGetTicketForQrAsOwner_thenTicketIsReturned() {
        when(ticketRepository.findWithDetailsByCode("test-code")).thenReturn(Optional.of(testTicket));

        Ticket result = ticketService.getTicketForQr("test-code", testOwner);

        assertNotNull(result);
        assertEquals("test-code", result.getCode());
        verify(ticketRepository).findWithDetailsByCode("test-code");
    }

    @Test
    void whenGetTicketForQrAsAdmin_thenTicketIsReturned() {
        User admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setRole(Role.ADMIN);

        when(ticketRepository.findWithDetailsByCode("test-code")).thenReturn(Optional.of(testTicket));

        Ticket result = ticketService.getTicketForQr("test-code", admin);

        assertNotNull(result);
        assertEquals("test-code", result.getCode());
        verify(ticketRepository).findWithDetailsByCode("test-code");
    }

    @Test
    void whenGetTicketForQrAsNonOwnerNonAdmin_thenExceptionIsThrown() {
        User requester = new User();
        requester.setId(UUID.randomUUID());
        requester.setRole(Role.USER);

        when(ticketRepository.findWithDetailsByCode("test-code")).thenReturn(Optional.of(testTicket));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> ticketService.getTicketForQr("test-code", requester));

        assertEquals("Нямаш право да виждаш този билет", exception.getMessage());
        verify(ticketRepository).findWithDetailsByCode("test-code");
    }

    @Test
    void whenGetTicketForQrWithNonExistingCode_thenExceptionIsThrown() {
        User requester = new User();
        requester.setId(UUID.randomUUID());

        when(ticketRepository.findWithDetailsByCode("non-existing")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ticketService.getTicketForQr("non-existing", requester));

        assertEquals("Билетът не беше намерен", exception.getMessage());
        verify(ticketRepository).findWithDetailsByCode("non-existing");
    }

    @Test
    void whenDeleteBySubscriptionId_thenRepositoryMethodIsCalled() {
        UUID subscriptionId = UUID.randomUUID();

        ticketService.deleteBySubscriptionId(subscriptionId);

        verify(ticketRepository).deleteBySubscriptionId(subscriptionId);
    }
}

