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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.validation.BindingResult;

import java.time.LocalDateTime;
import java.util.Optional;
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
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private EventService eventService;

    @Test
    void validateSchedule_WithValidTimes_ShouldNotAddErrors() {
        // Given
        EventCreateRequest request = new EventCreateRequest();
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(2));

        when(bindingResult.hasErrors()).thenReturn(false);

        // When
        eventService.validateSchedule(request, bindingResult);

        // Then
        verify(bindingResult, never()).rejectValue(anyString(), anyString(), anyString());
    }

    @Test
    void validateSchedule_WithEndTimeBeforeStartTime_ShouldAddError() {
        // Given
        EventCreateRequest request = new EventCreateRequest();
        request.setStartTime(LocalDateTime.now().plusDays(2));
        request.setEndTime(LocalDateTime.now().plusDays(1));

        when(bindingResult.hasErrors()).thenReturn(false);

        // When
        eventService.validateSchedule(request, bindingResult);

        // Then
        verify(bindingResult).rejectValue("endTime", "event.endTime.invalid", "Краят трябва да е след началото");
    }

    @Test
    void validateSchedule_WithBindingResultHasErrors_ShouldReturnEarly() {
        // Given
        EventCreateRequest request = new EventCreateRequest();
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(2));

        when(bindingResult.hasErrors()).thenReturn(true);

        // When
        eventService.validateSchedule(request, bindingResult);

        // Then
        verify(bindingResult, never()).rejectValue(anyString(), anyString(), anyString());
    }

    @Test
    void getById_WithExistingEvent_ShouldReturnEvent() {
        // Given
        UUID eventId = UUID.randomUUID();
        Event event = new Event();
        event.setId(eventId);
        event.setName("Test Event");

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // When
        Event result = eventService.getById(eventId);

        // Then
        assertNotNull(result);
        assertEquals(eventId, result.getId());
        assertEquals("Test Event", result.getName());
        verify(eventRepository).findById(eventId);
    }

    @Test
    void getById_WithNonExistingEvent_ShouldThrowException() {
        // Given
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.getById(eventId)
        );

        assertEquals("Събитието не е намерено", exception.getMessage());
        verify(eventRepository).findById(eventId);
    }

    @Test
    void create_WithValidRequest_ShouldCreateAndReturnEvent() {
        // Given
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

        // When
        Event result = eventService.create(request, user);

        // Then
        assertNotNull(result);
        assertEquals(request.getName(), result.getName());
        assertEquals(user, result.getCreator());
        assertEquals(category, result.getCategory());
        verify(categoryService).getActiveById(category.getId());
        verify(eventRepository).save(any(Event.class));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void create_WithNullCreator_ShouldThrowException() {
        // Given
        EventCreateRequest request = new EventCreateRequest();
        request.setName("Test Event");
        request.setCategoryId(UUID.randomUUID());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.create(request, null)
        );

        assertEquals("Организаторът е задължителен", exception.getMessage());
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void create_WithNullCategoryId_ShouldThrowException() {
        // Given
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setUsername("testuser");

        EventCreateRequest request = new EventCreateRequest();
        request.setName("Test Event");
        request.setCategoryId(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.create(request, user)
        );

        assertEquals("Категорията е задължителна", exception.getMessage());
        verify(eventRepository, never()).save(any(Event.class));
    }
}
