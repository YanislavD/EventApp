package main.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ErrorPageController.class, excludeAutoConfiguration = org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class)
class ErrorPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenGetOops_thenOopsPageIsShown() throws Exception {
        mockMvc.perform(get("/error/oops"))
                .andExpect(status().isOk())
                .andExpect(view().name("error/oops"))
                .andExpect(model().attribute("title", "Нещо се обърка"))
                .andExpect(model().attribute("message", "Изглежда се опитваш да направиш нещо, което не е позволено."));
    }
}

