package main.service;

import main.model.Category;
import main.model.Event;
import main.model.User;
import main.repository.EventRepository;
import main.web.dto.EventCreateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;

import java.time.LocalDateTime;
import java.util.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private TicketService ticketService;

    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private EventService eventService;

    @Test
    void validateSchedule_WithValidTimes_ShouldNotAddErrors() {

        EventCreateRequest request = new EventCreateRequest();
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(2));

        when(bindingResult.hasErrors()).thenReturn(false);

        eventService.validateSchedule(request, bindingResult);

        verify(bindingResult, never()).rejectValue(anyString(), anyString(), anyString());
    }

    @Test
    void validateSchedule_WithEndTimeBeforeStartTime_ShouldAddError() {

        EventCreateRequest request = new EventCreateRequest();
        request.setStartTime(LocalDateTime.now().plusDays(2));
        request.setEndTime(LocalDateTime.now().plusDays(1));

        when(bindingResult.hasErrors()).thenReturn(false);

        eventService.validateSchedule(request, bindingResult);

        verify(bindingResult).rejectValue("endTime", "event.endTime.invalid", "Краят трябва да е след началото");
    }

    @Test
    void validateSchedule_WithBindingResultHasErrors_ShouldReturnEarly() {

        EventCreateRequest request = new EventCreateRequest();
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(2));

        when(bindingResult.hasErrors()).thenReturn(true);


        eventService.validateSchedule(request, bindingResult);


        verify(bindingResult, never()).rejectValue(anyString(), anyString(), anyString());
    }

    @Test
    void getById_WithExistingEvent_ShouldReturnEvent() {

        UUID eventId = UUID.randomUUID();
        Event event = new Event();
        event.setId(eventId);
        event.setName("Test Event");

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        Event result = eventService.getById(eventId);

        assertNotNull(result);
        assertEquals(eventId, result.getId());
        assertEquals("Test Event", result.getName());
        verify(eventRepository).findById(eventId);
    }

    @Test
    void getById_WithNonExistingEvent_ShouldThrowException() {

        UUID eventId = UUID.randomUUID();
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.getById(eventId)
        );

        assertEquals("Събитието не е намерено", exception.getMessage());
        verify(eventRepository).findById(eventId);
    }

    @Test
    void create_WithValidRequest_ShouldCreateAndReturnEvent() {

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setUsername("testuser");

        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Test Category");
        category.setIsActive(true);

        EventCreateRequest request = new EventCreateRequest();
        request.setName("Test Event");
        request.setDescription("Test Description");
        request.setLocation("Test Location");
        request.setLatitude(42.6977);
        request.setLongitude(23.3219);
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(2));
        request.setCapacity(100);
        request.setCategoryId(category.getId());

        when(categoryService.getActiveById(category.getId())).thenReturn(category);
        
        Event savedEvent = new Event();
        savedEvent.setId(UUID.randomUUID());
        savedEvent.setName(request.getName());
        savedEvent.setDescription(request.getDescription());
        savedEvent.setCreator(user);
        savedEvent.setCategory(category);
        
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        Event result = eventService.create(request, user);

        assertNotNull(result);
        assertEquals(request.getName(), result.getName());
        assertEquals(user, result.getCreator());
        assertEquals(category, result.getCategory());
        verify(categoryService).getActiveById(category.getId());
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void create_WithNullCreator_ShouldThrowException() {

        EventCreateRequest request = new EventCreateRequest();
        request.setName("Test Event");
        request.setCategoryId(UUID.randomUUID());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.create(request, null)
        );

        assertEquals("Организаторът е задължителен", exception.getMessage());
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void create_WithNullCategoryId_ShouldThrowException() {

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setUsername("testuser");

        EventCreateRequest request = new EventCreateRequest();
        request.setName("Test Event");
        request.setCategoryId(null);
        
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.create(request, user)
        );

        assertEquals("Категорията е задължителна", exception.getMessage());
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void unsubscribeUserFromEvent_WhenNotSubscribed_ShouldReturnFalse() {
        User user = new User();
        user.setId(UUID.randomUUID());

        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setName("Test Event");

        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(subscriptionService.existsByUserAndEvent(user.getId(), event.getId())).thenReturn(false);

        boolean result = eventService.unsubscribeUserFromEvent(event.getId(), user);

        assertFalse(result);
        verify(subscriptionService, never()).deleteByUserAndEvent(any(), any());
    }

    @Test
    void unsubscribeUserFromEvent_WhenSubscribed_ShouldUnsubscribe() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");

        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setName("Test Event");

        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(subscriptionService.existsByUserAndEvent(user.getId(), event.getId())).thenReturn(true);

        boolean result = eventService.unsubscribeUserFromEvent(event.getId(), user);

        assertTrue(result);
        verify(subscriptionService).deleteByUserAndEvent(user.getId(), event.getId());
    }

    @Test
    void deleteEventsOlderThanDays_ShouldDeleteOldEvents() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(2);

        Event oldEvent1 = new Event();
        oldEvent1.setId(UUID.randomUUID());

        Event oldEvent2 = new Event();
        oldEvent2.setId(UUID.randomUUID());

        List<Event> oldEvents = Arrays.asList(oldEvent1, oldEvent2);

        when(eventRepository.findEventsOlderThan(any(LocalDateTime.class))).thenReturn(oldEvents);

        int result = eventService.deleteEventsOlderThanDays(2);

        assertEquals(2, result);
        verify(subscriptionService).deleteAllByEventId(oldEvent1.getId());
        verify(subscriptionService).deleteAllByEventId(oldEvent2.getId());
        verify(eventRepository).delete(oldEvent1);
        verify(eventRepository).delete(oldEvent2);
    }

    @Test
    void subscribeUserToEvent_WhenUserIsCreator_ShouldThrowException() {
        User creator = new User();
        creator.setId(UUID.randomUUID());

        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setCreator(creator);

        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> eventService.subscribeUserToEvent(event.getId(), creator)
        );

        assertEquals("OWN_EVENT", exception.getMessage());
        verify(subscriptionService, never()).create(any(), any());
    }

    @Test
    void subscribeUserToEvent_WhenEventIsFull_ShouldThrowException() {
        User user = new User();
        user.setId(UUID.randomUUID());

        User creator = new User();
        creator.setId(UUID.randomUUID());

        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setCreator(creator);
        event.setCapacity(10);

        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(subscriptionService.existsByUserAndEvent(user.getId(), event.getId())).thenReturn(false);
        when(subscriptionService.countByEvent(event.getId())).thenReturn(10L);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> eventService.subscribeUserToEvent(event.getId(), user)
        );

        assertEquals("FULL", exception.getMessage());
        verify(subscriptionService, never()).create(any(), any());
    }

    @Test
    void deleteAllByCreatorId_ShouldDeleteAllEvents() {
        UUID creatorId = UUID.randomUUID();

        Event event1 = new Event();
        event1.setId(UUID.randomUUID());
        event1.setName("Event 1");

        Event event2 = new Event();
        event2.setId(UUID.randomUUID());
        event2.setName("Event 2");

        List<Event> events = Arrays.asList(event1, event2);

        when(eventRepository.findByCreatorId(creatorId)).thenReturn(events);

        eventService.deleteAllByCreatorId(creatorId);

        verify(subscriptionService).deleteAllByEventId(event1.getId());
        verify(subscriptionService).deleteAllByEventId(event2.getId());
        verify(eventRepository).delete(event1);
        verify(eventRepository).delete(event2);
    }

    @Test
    void buildEditRequest_WithValidEventAndOwner_ShouldReturnRequest() {
        User user = new User();
        user.setId(UUID.randomUUID());

        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Test Category");

        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setName("Test Event");
        event.setDescription("Test Description");
        event.setCreator(user);
        event.setCategory(category);

        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

        EventCreateRequest result = eventService.buildEditRequest(event.getId(), user);

        assertNotNull(result);
        assertEquals("Test Event", result.getName());
        assertEquals("Test Description", result.getDescription());
        assertEquals(category.getId(), result.getCategoryId());
        verify(eventRepository).findById(event.getId());
    }

    @Test
    void buildEditRequest_WithNonOwner_ShouldThrowException() {
        User owner = new User();
        owner.setId(UUID.randomUUID());

        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());

        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setCreator(owner);

        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> eventService.buildEditRequest(event.getId(), otherUser)
        );

        assertEquals("Можеш да редактираш само събития, които си създал", exception.getMessage());
    }

    @Test
    void getCount_ShouldReturnRepositoryCount() {
        when(eventRepository.count()).thenReturn(10L);

        Long result = eventService.getCount();

        assertEquals(10L, result);
        verify(eventRepository).count();
    }

    @Test
    void getAvailableCategories_ShouldReturnActiveCategories() {
        List<Category> categories = new ArrayList<>();
        Category category = new Category();
        category.setName("Test Category");
        categories.add(category);

        when(categoryService.getAllActive()).thenReturn(categories);

        List<Category> result = eventService.getAvailableCategories();

        assertEquals(1, result.size());
        assertEquals("Test Category", result.get(0).getName());
        verify(categoryService).getAllActive();
    }

    @Test
    void getEventsForListing_ShouldReturnEventViews() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setName("Test Event");
        event.setStartTime(now.plusDays(1));

        Category category = new Category();
        category.setId(categoryId);
        event.setCategory(category);

        List<Event> events = Arrays.asList(event);
        Set<UUID> subscribedEventIds = new HashSet<>();

        when(eventRepository.findUpcomingEvents(any(LocalDateTime.class))).thenReturn(events);
        when(subscriptionService.getSubscribedEventIds(userId)).thenReturn(subscribedEventIds);

        List<main.web.view.EventView> result = eventService.getEventsForListing(userId, categoryId);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventRepository).findUpcomingEvents(any(LocalDateTime.class));
        verify(subscriptionService).getSubscribedEventIds(userId);
    }

    @Test
    void getEventsForListing_WithNullCategoryFilter_ShouldReturnAllEvents() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Event event1 = new Event();
        event1.setId(UUID.randomUUID());
        event1.setName("Event 1");
        event1.setStartTime(now.plusDays(1));

        Event event2 = new Event();
        event2.setId(UUID.randomUUID());
        event2.setName("Event 2");
        event2.setStartTime(now.plusDays(2));

        List<Event> events = Arrays.asList(event1, event2);
        Set<UUID> subscribedEventIds = new HashSet<>();

        when(eventRepository.findUpcomingEvents(any(LocalDateTime.class))).thenReturn(events);
        when(subscriptionService.getSubscribedEventIds(userId)).thenReturn(subscribedEventIds);

        List<main.web.view.EventView> result = eventService.getEventsForListing(userId, null);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(eventRepository).findUpcomingEvents(any(LocalDateTime.class));
        verify(subscriptionService).getSubscribedEventIds(userId);
    }

    @Test
    void getCreatedEvents_ShouldReturnUserCreatedEvents() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        User creator = new User();
        creator.setId(userId);

        Event event1 = new Event();
        event1.setId(UUID.randomUUID());
        event1.setName("Event 1");
        event1.setCreator(creator);
        event1.setStartTime(now.plusDays(1));
        event1.setEndTime(now.plusDays(2));

        Event event2 = new Event();
        event2.setId(UUID.randomUUID());
        event2.setName("Event 2");
        event2.setCreator(creator);
        event2.setStartTime(now.plusDays(3));
        event2.setEndTime(now.plusDays(4));

        List<Event> allEvents = Arrays.asList(event1, event2);

        when(eventRepository.findAll()).thenReturn(allEvents);

        List<main.web.view.EventView> result = eventService.getCreatedEvents(userId);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(eventRepository).findAll();
    }
}
