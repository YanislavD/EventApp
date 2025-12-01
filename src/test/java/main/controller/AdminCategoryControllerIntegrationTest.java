package main.controller;

import main.model.Category;
import main.model.Role;
import main.model.User;
import main.repository.CategoryRepository;
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
class AdminCategoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private Category testCategory;

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

        testCategory = new Category();
        testCategory.setName("Test Category");
        testCategory.setIsActive(true);
        testCategory = categoryRepository.save(testCategory);
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void whenGetCategories_thenCategoriesPageIsShown() throws Exception {
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-categories"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(model().attributeExists("categoryCreateRequest"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void whenCreateCategoryWithValidData_thenCategoryIsCreated() throws Exception {
        mockMvc.perform(post("/admin/categories")
                        .with(csrf())
                        .param("name", "New Category"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"));

        Category savedCategory = categoryRepository.findByNameIgnoreCase("New Category").orElse(null);
        assertNotNull(savedCategory);
        assertEquals("New Category", savedCategory.getName());
        assertTrue(savedCategory.getIsActive());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void whenCreateCategoryWithInvalidData_thenValidationError() throws Exception {
        mockMvc.perform(post("/admin/categories")
                        .with(csrf())
                        .param("name", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-categories"))
                .andExpect(model().attributeHasErrors("categoryCreateRequest"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void whenDeleteCategory_thenCategoryIsDeactivated() throws Exception {
        UUID categoryId = testCategory.getId();

        mockMvc.perform(delete("/admin/categories/{id}", categoryId)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"));

        Category deactivatedCategory = categoryRepository.findById(categoryId).orElse(null);
        assertNotNull(deactivatedCategory);
        assertFalse(deactivatedCategory.getIsActive());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void whenActivateCategory_thenCategoryIsActivated() throws Exception {
        Category inactiveCategory = new Category();
        inactiveCategory.setName("Inactive Category");
        inactiveCategory.setIsActive(false);
        inactiveCategory = categoryRepository.save(inactiveCategory);

        mockMvc.perform(post("/admin/categories/{id}", inactiveCategory.getId())
                        .with(csrf())
                        .param("action", "activate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"));

        Category activatedCategory = categoryRepository.findById(inactiveCategory.getId()).orElse(null);
        assertNotNull(activatedCategory);
        assertTrue(activatedCategory.getIsActive());
    }
}

