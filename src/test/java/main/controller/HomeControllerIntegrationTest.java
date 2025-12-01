package main.controller;

import main.model.Role;
import main.model.User;
import main.repository.UserRepository;
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
class HomeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private User otherUser;

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

        otherUser = new User();
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@example.com");
        otherUser.setPassword(passwordEncoder.encode("password123"));
        otherUser.setRole(Role.USER);
        otherUser.setCreatedOn(LocalDateTime.now());
        otherUser.setUpdatedOn(LocalDateTime.now());
        otherUser = userRepository.save(otherUser);
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void whenGetHome_thenHomePageIsShown() throws Exception {
        mockMvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("subscribedEvents"))
                .andExpect(model().attributeExists("createdEvents"))
                .andExpect(model().attributeExists("pastSubscribedEvents"))
                .andExpect(model().attributeExists("pastCreatedEvents"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void whenGetProfile_thenProfilePageIsShown() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void whenGetOwnProfileEdit_thenEditFormIsShown() throws Exception {
        mockMvc.perform(get("/profile/{id}", testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("profile-edit"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void whenGetOtherUserProfileEdit_thenRedirectToProfile() throws Exception {
        mockMvc.perform(get("/profile/{id}", otherUser.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void whenUpdateProfile_thenProfileIsUpdated() throws Exception {
        String newFirstName = "John";
        String newLastName = "Doe";

        mockMvc.perform(post("/profile/{id}", testUser.getId())
                        .with(csrf())
                        .param("firstName", newFirstName)
                        .param("lastName", newLastName))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));

        User updatedUser = userRepository.findById(testUser.getId()).orElse(null);
        assertNotNull(updatedUser);
        assertEquals(newFirstName, updatedUser.getFirstName());
        assertEquals(newLastName, updatedUser.getLastName());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void whenGetEvents_thenEventsPageIsShown() throws Exception {
        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(view().name("events"))
                .andExpect(model().attributeExists("events"))
                .andExpect(model().attributeExists("pastEvents"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void whenGetEventsWithCategoryFilter_thenFilteredEventsAreShown() throws Exception {
        UUID categoryId = UUID.randomUUID();
        mockMvc.perform(get("/events")
                        .param("category", categoryId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("events"))
                .andExpect(model().attributeExists("selectedCategory"))
                .andExpect(model().attribute("selectedCategory", categoryId));
    }
}

