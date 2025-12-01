package main.controller;

import main.model.Category;
import main.model.Event;
import main.model.Role;
import main.model.User;
import main.repository.CategoryRepository;
import main.repository.EventRepository;
import main.repository.UserRepository;
import main.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EventControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EventService eventService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setRole(Role.USER);
        testUser.setCreatedOn(LocalDateTime.now());
        testUser.setUpdatedOn(LocalDateTime.now());
        testUser = userRepository.save(testUser);

        testCategory = new Category();
        testCategory.setName("Test Category");
        testCategory.setIsActive(true);
        testCategory = categoryRepository.save(testCategory);
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void whenCreateEventWithValidData_thenEventIsCreated() throws Exception {
        mockMvc.perform(post("/events")
                        .with(csrf())
                        .param("name", "Test Event")
                        .param("description", "Test Description")
                        .param("location", "Test Location")
                        .param("latitude", "42.6977")
                        .param("longitude", "23.3219")
                        .param("startTime", LocalDateTime.now().plusDays(1).toString())
                        .param("endTime", LocalDateTime.now().plusDays(2).toString())
                        .param("capacity", "100")
                        .param("categoryId", testCategory.getId().toString())
                        .param("imageName", "event-business.jpg"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events"));

        Event savedEvent = eventRepository.findAll().stream()
                .filter(e -> e.getName().equals("Test Event"))
                .findFirst()
                .orElse(null);

        assertNotNull(savedEvent);
        assertEquals("Test Event", savedEvent.getName());
        assertEquals(testUser.getId(), savedEvent.getCreator().getId());
        assertEquals(testCategory.getId(), savedEvent.getCategory().getId());
    }

    @Test
    @WithMockUser(username = "subscriber@example.com")
    void whenSubscribeToEvent_thenUserIsSubscribed() throws Exception {

        User subscriber = new User();
        subscriber.setUsername("subscriber");
        subscriber.setEmail("subscriber@example.com");
        subscriber.setPassword(passwordEncoder.encode("password123"));
        subscriber.setRole(Role.USER);
        subscriber.setCreatedOn(LocalDateTime.now());
        subscriber.setUpdatedOn(LocalDateTime.now());
        subscriber = userRepository.save(subscriber);

        Event event = createTestEvent();

        mockMvc.perform(post("/events/{eventId}/subscriptions", event.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events"));

        boolean isSubscribed = eventService.getSubscribedEvents(subscriber.getId()).stream()
                .anyMatch(e -> e.getId().equals(event.getId()));

        assertTrue(isSubscribed);
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void whenUpdateEventWithValidData_thenEventIsUpdated() throws Exception {
        Event event = createTestEvent();

        String updatedName = "Updated Event Name";
        String updatedDescription = "Updated Description";

        mockMvc.perform(put("/events/{eventId}", event.getId())
                        .with(csrf())
                        .param("name", updatedName)
                        .param("description", updatedDescription)
                        .param("location", "Updated Location")
                        .param("latitude", "42.6977")
                        .param("longitude", "23.3219")
                        .param("startTime", LocalDateTime.now().plusDays(1).toString())
                        .param("endTime", LocalDateTime.now().plusDays(2).toString())
                        .param("capacity", "150")
                        .param("categoryId", testCategory.getId().toString())
                        .param("imageName", "event-workshop.jpg"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));

        Event updatedEvent = eventRepository.findById(event.getId()).orElse(null);
        assertNotNull(updatedEvent);
        assertEquals(updatedName, updatedEvent.getName());
        assertEquals(updatedDescription, updatedEvent.getDescription());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void whenDeleteEvent_thenEventIsDeleted() throws Exception {
        Event event = createTestEvent();
        UUID eventId = event.getId();

        mockMvc.perform(delete("/events/{eventId}", eventId)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));

        assertFalse(eventRepository.findById(eventId).isPresent());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void whenGetEditForm_thenEditFormIsShown() throws Exception {
        Event event = createTestEvent();

        mockMvc.perform(get("/events/{eventId}/form", event.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("event-edit"))
                .andExpect(model().attributeExists("eventCreateRequest"))
                .andExpect(model().attributeExists("eventId"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void whenGetNewEventForm_thenCreateFormIsShown() throws Exception {
        mockMvc.perform(get("/events/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("event-create"))
                .andExpect(model().attributeExists("eventCreateRequest"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void whenCreateEventWithInvalidSchedule_thenValidationError() throws Exception {
        mockMvc.perform(post("/events")
                        .with(csrf())
                        .param("name", "Test Event")
                        .param("startTime", LocalDateTime.now().plusDays(2).toString())
                        .param("endTime", LocalDateTime.now().plusDays(1).toString())
                        .param("categoryId", testCategory.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("event-create"))
                .andExpect(model().attributeHasErrors("eventCreateRequest"));
    }

    private Event createTestEvent() {
        Event event = new Event();
        event.setName("Test Event");
        event.setDescription("Test Description");
        event.setLocation("Test Location");
        event.setLatitude(42.6977);
        event.setLongitude(23.3219);
        event.setStartTime(LocalDateTime.now().plusDays(1));
        event.setEndTime(LocalDateTime.now().plusDays(2));
        event.setCapacity(100);
        event.setCategory(testCategory);
        event.setCreator(testUser);
        event.setImageName("event-business.jpg");
        return eventRepository.save(event);
    }
}

