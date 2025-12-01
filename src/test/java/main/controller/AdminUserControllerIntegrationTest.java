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
class AdminUserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private User regularUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setUsername("adminuser");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword(passwordEncoder.encode("password123"));
        adminUser.setRole(Role.ADMIN);
        adminUser.setCreatedOn(LocalDateTime.now());
        adminUser.setUpdatedOn(LocalDateTime.now());
        adminUser = userRepository.save(adminUser);

        regularUser = new User();
        regularUser.setUsername("regularuser");
        regularUser.setEmail("regular@example.com");
        regularUser.setPassword(passwordEncoder.encode("password123"));
        regularUser.setRole(Role.USER);
        regularUser.setCreatedOn(LocalDateTime.now());
        regularUser.setUpdatedOn(LocalDateTime.now());
        regularUser = userRepository.save(regularUser);
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void whenGetUsers_thenUsersPageIsShown() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-users"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attributeExists("roles"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void whenChangeUserRole_thenRoleIsUpdated() throws Exception {
        mockMvc.perform(post("/admin/users/{userId}/role", regularUser.getId())
                        .with(csrf())
                        .param("role", "ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        User updatedUser = userRepository.findById(regularUser.getId()).orElse(null);
        assertNotNull(updatedUser);
        assertEquals(Role.ADMIN, updatedUser.getRole());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void whenChangeOwnRole_thenRedirectToLogout() throws Exception {
        mockMvc.perform(post("/admin/users/{userId}/role", adminUser.getId())
                        .with(csrf())
                        .param("role", "USER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/logout"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void whenDeleteUser_thenUserIsDeleted() throws Exception {
        UUID userIdToDelete = regularUser.getId();

        mockMvc.perform(post("/admin/users/{userId}", userIdToDelete)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        assertFalse(userRepository.findById(userIdToDelete).isPresent());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void whenDeleteOwnUser_thenError() throws Exception {
        mockMvc.perform(post("/admin/users/{userId}", adminUser.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        assertTrue(userRepository.findById(adminUser.getId()).isPresent());
    }
}

