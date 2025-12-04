package main.service;

import main.model.Event;
import main.model.Subscription;
import main.model.User;
import main.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private TicketService ticketService;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private User testUser;
    private Event testEvent;
    private Subscription testSubscription;
    private UUID testUserId;
    private UUID testEventId;
    private UUID testSubscriptionId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testEventId = UUID.randomUUID();
        testSubscriptionId = UUID.randomUUID();

        testUser = new User();
        testUser.setId(testUserId);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testEvent = new Event();
        testEvent.setId(testEventId);
        testEvent.setName("Test Event");

        testSubscription = Subscription.builder()
                .id(testSubscriptionId)
                .user(testUser)
                .event(testEvent)
                .subscriptionTime(LocalDateTime.now())
                .build();
    }

    @Test
    void whenFindByUserId_thenSubscriptionsAreReturned() {
        List<Subscription> subscriptions = new ArrayList<>();
        subscriptions.add(testSubscription);

        when(subscriptionRepository.findByUserId(testUserId)).thenReturn(subscriptions);

        List<Subscription> result = subscriptionService.findByUserId(testUserId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testSubscriptionId, result.get(0).getId());
        verify(subscriptionRepository).findByUserId(testUserId);
    }

    @Test
    void whenGetSubscribedEventIds_thenEventIdsAreReturned() {
        List<Subscription> subscriptions = new ArrayList<>();
        subscriptions.add(testSubscription);

        when(subscriptionRepository.findByUserId(testUserId)).thenReturn(subscriptions);

        Set<UUID> result = subscriptionService.getSubscribedEventIds(testUserId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(testEventId));
        verify(subscriptionRepository).findByUserId(testUserId);
    }

    @Test
    void whenGetSubscribedEventIdsWithNullEvent_thenNullEventIsFiltered() {
        Subscription subscriptionWithNullEvent = Subscription.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .event(null)
                .subscriptionTime(LocalDateTime.now())
                .build();

        List<Subscription> subscriptions = new ArrayList<>();
        subscriptions.add(testSubscription);
        subscriptions.add(subscriptionWithNullEvent);

        when(subscriptionRepository.findByUserId(testUserId)).thenReturn(subscriptions);

        Set<UUID> result = subscriptionService.getSubscribedEventIds(testUserId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(testEventId));
        verify(subscriptionRepository).findByUserId(testUserId);
    }

    @Test
    void whenExistsByUserAndEvent_thenTrueIsReturned() {
        when(subscriptionRepository.existsByUserIdAndEventId(testUserId, testEventId)).thenReturn(true);

        boolean result = subscriptionService.existsByUserAndEvent(testUserId, testEventId);

        assertTrue(result);
        verify(subscriptionRepository).existsByUserIdAndEventId(testUserId, testEventId);
    }

    @Test
    void whenExistsByUserAndEvent_thenFalseIsReturned() {
        when(subscriptionRepository.existsByUserIdAndEventId(testUserId, testEventId)).thenReturn(false);

        boolean result = subscriptionService.existsByUserAndEvent(testUserId, testEventId);

        assertFalse(result);
        verify(subscriptionRepository).existsByUserIdAndEventId(testUserId, testEventId);
    }

    @Test
    void whenCountByEvent_thenCountIsReturned() {
        when(subscriptionRepository.countByEventId(testEventId)).thenReturn(5L);

        long result = subscriptionService.countByEvent(testEventId);

        assertEquals(5L, result);
        verify(subscriptionRepository).countByEventId(testEventId);
    }

    @Test
    void whenFindByEventId_thenSubscriptionsAreReturned() {
        List<Subscription> subscriptions = new ArrayList<>();
        subscriptions.add(testSubscription);

        when(subscriptionRepository.findByEventId(testEventId)).thenReturn(subscriptions);

        List<Subscription> result = subscriptionService.findByEventId(testEventId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testSubscriptionId, result.get(0).getId());
        verify(subscriptionRepository).findByEventId(testEventId);
    }

    @Test
    void whenCreateWithValidData_thenSubscriptionIsCreated() {
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

        Subscription result = subscriptionService.create(testUser, testEvent);

        assertNotNull(result);
        verify(subscriptionRepository).save(any(Subscription.class));
        verify(ticketService).issueTicket(any(Subscription.class));
    }

    @Test
    void whenCreateWithNullUser_thenExceptionIsThrown() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> subscriptionService.create(null, testEvent));

        assertEquals("Потребителят е задължителен", exception.getMessage());
        verify(subscriptionRepository, never()).save(any());
        verify(ticketService, never()).issueTicket(any());
    }

    @Test
    void whenCreateWithNullEvent_thenExceptionIsThrown() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> subscriptionService.create(testUser, null));

        assertEquals("Събитието е задължително", exception.getMessage());
        verify(subscriptionRepository, never()).save(any());
        verify(ticketService, never()).issueTicket(any());
    }

    @Test
    void whenDeleteByUserAndEventWithExistingSubscription_thenSubscriptionIsDeleted() {
        when(subscriptionRepository.findByUserIdAndEventId(testUserId, testEventId))
                .thenReturn(Optional.of(testSubscription));

        subscriptionService.deleteByUserAndEvent(testUserId, testEventId);

        verify(ticketService).deleteBySubscriptionId(testSubscriptionId);
        verify(subscriptionRepository).delete(testSubscription);
    }

    @Test
    void whenDeleteByUserAndEventWithNonExistentSubscription_thenNothingHappens() {
        when(subscriptionRepository.findByUserIdAndEventId(testUserId, testEventId))
                .thenReturn(Optional.empty());

        subscriptionService.deleteByUserAndEvent(testUserId, testEventId);

        verify(ticketService, never()).deleteBySubscriptionId(any());
        verify(subscriptionRepository, never()).delete(any());
    }

    @Test
    void whenDeleteAllByEventId_thenAllSubscriptionsAreDeleted() {
        List<Subscription> subscriptions = new ArrayList<>();
        subscriptions.add(testSubscription);

        when(subscriptionRepository.findByEventId(testEventId)).thenReturn(subscriptions);

        subscriptionService.deleteAllByEventId(testEventId);

        verify(ticketService).deleteBySubscriptionId(testSubscriptionId);
        verify(subscriptionRepository).delete(testSubscription);
    }

    @Test
    void whenDeleteAllByUserId_thenAllSubscriptionsAreDeleted() {
        List<Subscription> subscriptions = new ArrayList<>();
        subscriptions.add(testSubscription);

        when(subscriptionRepository.findByUserId(testUserId)).thenReturn(subscriptions);

        subscriptionService.deleteAllByUserId(testUserId);

        verify(ticketService).deleteBySubscriptionId(testSubscriptionId);
        verify(subscriptionRepository).delete(testSubscription);
    }
}

