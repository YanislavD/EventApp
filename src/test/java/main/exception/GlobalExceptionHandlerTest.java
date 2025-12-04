package main.exception;

import main.controller.AdminCategoryController;
import main.controller.IndexController;
import main.controller.RatingController;
import main.service.CategoryService;
import main.service.EventService;
import main.service.RatingService;
import main.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {IndexController.class, AdminCategoryController.class, RatingController.class}, excludeAutoConfiguration = org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private EventService eventService;

    @MockBean
    private RatingService ratingService;

    @Test
    void whenUserAlreadyExistsException_thenRedirectToRegister() throws Exception {
        doThrow(new UserAlreadyExistsException("Потребителското име или имейл вече е заето"))
                .when(userService).register(any());

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "existinguser")
                        .param("email", "existing@example.com")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"));
    }

    @Test
    void whenCategoryAlreadyExistsException_thenRedirectToAdminCategories() throws Exception {
        doThrow(new CategoryAlreadyExistsException("Категорията вече съществува"))
                .when(categoryService).create(anyString());

        mockMvc.perform(post("/admin/categories")
                        .with(csrf())
                        .param("name", "Existing Category"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"));
    }

    @Test
    void whenIllegalArgumentException_thenShow404Page() throws Exception {
        when(categoryService.getAll()).thenReturn(new java.util.ArrayList<>());
        doThrow(new IllegalArgumentException("Невалиден аргумент"))
                .when(categoryService).deleteById(any(java.util.UUID.class));

        mockMvc.perform(delete("/admin/categories/00000000-0000-0000-0000-000000000000")
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(model().attributeExists("message"));
    }

    @Test
    void whenIllegalStateException_thenShowOopsPage() throws Exception {
        when(categoryService.getAll()).thenReturn(new java.util.ArrayList<>());
        doThrow(new IllegalStateException("Невалидно състояние"))
                .when(categoryService).activateById(any(java.util.UUID.class));

        mockMvc.perform(post("/admin/categories/00000000-0000-0000-0000-000000000000")
                        .with(csrf())
                        .param("action", "activate"))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/oops"))
                .andExpect(model().attributeExists("message"));
    }

    @Test
    void whenIllegalArgumentExceptionFromRatings_thenRedirectToHome() throws Exception {
        main.model.User mockUser = new main.model.User();
        mockUser.setId(java.util.UUID.randomUUID());
        when(userService.getByEmail(anyString())).thenReturn(mockUser);
        
        doThrow(new IllegalArgumentException("Невалиден event ID"))
                .when(ratingService).createRating(any(java.util.UUID.class), any(java.util.UUID.class), anyInt());

        mockMvc.perform(post("/ratings")
                        .with(csrf())
                        .param("eventId", java.util.UUID.randomUUID().toString())
                        .param("score", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    @Test
    void whenIllegalStateExceptionFromRatings_thenRedirectToHome() throws Exception {
        main.model.User mockUser = new main.model.User();
        mockUser.setId(java.util.UUID.randomUUID());
        when(userService.getByEmail(anyString())).thenReturn(mockUser);
        
        doThrow(new IllegalStateException("Вече си оценил това събитие"))
                .when(ratingService).createRating(any(java.util.UUID.class), any(java.util.UUID.class), anyInt());

        mockMvc.perform(post("/ratings")
                        .with(csrf())
                        .param("eventId", java.util.UUID.randomUUID().toString())
                        .param("score", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    @Test
    void whenRuntimeExceptionFromRatings_thenRedirectToHome() throws Exception {
        main.model.User mockUser = new main.model.User();
        mockUser.setId(java.util.UUID.randomUUID());
        when(userService.getByEmail(anyString())).thenReturn(mockUser);
        
        doThrow(new RuntimeException("Грешка при създаване на рейтинг"))
                .when(ratingService).createRating(any(java.util.UUID.class), any(java.util.UUID.class), anyInt());

        mockMvc.perform(post("/ratings")
                        .with(csrf())
                        .param("eventId", java.util.UUID.randomUUID().toString())
                        .param("score", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    @Test
    void whenRuntimeExceptionFromNonRatings_thenExceptionIsRethrown() {
        when(categoryService.getAll()).thenThrow(new RuntimeException("Неочаквана грешка"));

        assertThrows(Exception.class, () -> {
            mockMvc.perform(get("/admin/categories"));
        });
    }

}

