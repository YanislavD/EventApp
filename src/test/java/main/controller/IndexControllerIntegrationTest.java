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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class IndexControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void whenGetIndex_thenIndexPageIsShown() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("eventsCounter"))
                .andExpect(model().attributeExists("usersCounter"))
                .andExpect(model().attributeExists("categoriesCounter"));
    }

    @Test
    void whenGetRegister_thenRegisterFormIsShown() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("registerRequest"));
    }

    @Test
    void whenGetLogin_thenLoginFormIsShown() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("loginRequest"));
    }

    @Test
    void whenRegisterWithValidData_thenUserIsRegistered() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "newuser")
                        .param("email", "newuser@example.com")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        User savedUser = userRepository.findByEmail("newuser@example.com");
        assertNotNull(savedUser);
        assertEquals("newuser", savedUser.getUsername());
        assertEquals("newuser@example.com", savedUser.getEmail());
        assertTrue(passwordEncoder.matches("password123", savedUser.getPassword()));
    }

    @Test
    void whenRegisterWithInvalidData_thenValidationError() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "user")
                        .param("email", "invalid-email")
                        .param("password", "123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasErrors("registerRequest"));
    }

    @Test
    void whenRegisterWithExistingEmail_thenError() throws Exception {
        User existingUser = new User();
        existingUser.setUsername("existing");
        existingUser.setEmail("existing@example.com");
        existingUser.setPassword(passwordEncoder.encode("password123"));
        existingUser.setRole(Role.USER);
        existingUser.setCreatedOn(LocalDateTime.now());
        existingUser.setUpdatedOn(LocalDateTime.now());
        userRepository.save(existingUser);

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "newuser")
                        .param("email", "existing@example.com")
                        .param("password", "password123"))
                .andExpect(status().is(302))
                .andExpect(view().name("redirect:/register"));
    }

    @Test
    void whenGetLogout_thenRedirectToIndex() throws Exception {
        mockMvc.perform(get("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }
}

