package main.controller;

import jakarta.validation.Valid;
import main.service.EventService;
import main.service.UserService;
import main.web.dto.LoginRequest;
import main.web.dto.RegisterRequest;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class IndexController {

    private final EventService eventService;
    private final UserService userService;

    public IndexController(EventService eventService, UserService userService) {
        this.eventService = eventService;
        this.userService = userService;
    }

    @GetMapping
    public ModelAndView index() {

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("index");
        modelAndView.addObject("eventsCounter", eventService.getCount());
        modelAndView.addObject("usersCounter", userService.getCount());
        modelAndView.addObject("categoriesCounter", eventService.getDistinctCategoryCount());
        return modelAndView;
    }

    @GetMapping("/register")
    public ModelAndView register() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("register");
        modelAndView.addObject("registerRequest", new RegisterRequest());
        return modelAndView;
    }

    @GetMapping("/login")
    public ModelAndView login() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("login");
        modelAndView.addObject("loginRequest",new LoginRequest());
        return modelAndView;
    }


    @GetMapping("/logout")
    public ModelAndView logout() {
        // Spring Security will handle the actual logout (intercepts GET /logout)
        // This mapping exists to satisfy routing and allows graceful redirect as fallback
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("redirect:/");
        return modelAndView;
    }

    @PostMapping("/register")
    public ModelAndView registerUser(@Valid RegisterRequest registerRequest, BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            ModelAndView mv = new ModelAndView("register");
            mv.addObject("registerRequest", registerRequest);
            return mv;
        }

        userService.register(registerRequest);
        return new ModelAndView("redirect:/login");
    }
}
