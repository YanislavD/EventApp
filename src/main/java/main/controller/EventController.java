package main.controller;

import jakarta.validation.Valid;
import main.model.Category;
import main.model.User;
import main.service.CategoryService;
import main.service.EventService;
import main.service.UserService;
import main.web.dto.EventCreateRequest;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;
    private final CategoryService categoryService;
    private final UserService userService;

    public EventController(EventService eventService, CategoryService categoryService, UserService userService) {
        this.eventService = eventService;
        this.categoryService = categoryService;
        this.userService = userService;
    }

    @ModelAttribute("categories")
    public List<Category> categories() {
        return categoryService.getAll();
    }

    @GetMapping("/create")
    public ModelAndView showCreateForm() {
        ModelAndView modelAndView = new ModelAndView("event-create");
        modelAndView.addObject("eventCreateRequest", new EventCreateRequest());
        return modelAndView;
    }

    @PostMapping("/create")
    public ModelAndView createEvent(@Valid @ModelAttribute("eventCreateRequest") EventCreateRequest eventCreateRequest,
                                    BindingResult bindingResult,
                                    Principal principal) {

        if (!bindingResult.hasErrors() && eventCreateRequest.getStartTime() != null && eventCreateRequest.getEndTime() != null) {
            if (!eventCreateRequest.getEndTime().isAfter(eventCreateRequest.getStartTime())) {
                bindingResult.rejectValue("endTime", "event.endTime.invalid", "Краят трябва да е след началото");
            }
        }

        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView("event-create");
            modelAndView.addObject("eventCreateRequest", eventCreateRequest);
            return modelAndView;
        }

        User creator = userService.getByEmail(principal.getName());
        eventService.create(eventCreateRequest, creator);
        return new ModelAndView("redirect:/events?created");
    }

    @PostMapping("/{eventId}/subscribe")
    public ModelAndView subscribeToEvent(@PathVariable UUID eventId, Principal principal) {
        User user = userService.getByEmail(principal.getName());

        try {
            boolean subscribed = eventService.subscribeUserToEvent(eventId, user);
            if (subscribed) {
                return new ModelAndView("redirect:/events?joined");
            }
            return new ModelAndView("redirect:/events?already");
        } catch (IllegalArgumentException ex) {
            return new ModelAndView("redirect:/events?missing");
        } catch (IllegalStateException ex) {
            String code = ex.getMessage();
            if ("OWN_EVENT".equals(code)) {
                return new ModelAndView("redirect:/events?error=own");
            }
            if ("FULL".equals(code)) {
                return new ModelAndView("redirect:/events?error=full");
            }
            return new ModelAndView("redirect:/events?error=unknown");
        }
    }
}

