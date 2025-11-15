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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

        eventService.validateSchedule(eventCreateRequest, bindingResult);

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
        boolean subscribed = eventService.subscribeUserToEvent(eventId, user);
        if (subscribed) {
            return new ModelAndView("redirect:/events?joined");
        }
        return new ModelAndView("redirect:/events?already");
    }

    @PostMapping("/{eventId}/unsubscribe")
    public ModelAndView unsubscribeFromEvent(@PathVariable UUID eventId, Principal principal) {
        User user = userService.getByEmail(principal.getName());
        boolean unsubscribed = eventService.unsubscribeUserFromEvent(eventId, user);
        if (unsubscribed) {
            return new ModelAndView("redirect:/home?unsubscribed");
        }
        return new ModelAndView("redirect:/home?notSubscribed");
    }

    @GetMapping("/{eventId}")
    public ModelAndView showEditForm(@PathVariable UUID eventId, Principal principal) {
        User user = userService.getByEmail(principal.getName());
        EventCreateRequest eventCreateRequest = eventService.buildEditRequest(eventId, user);

        ModelAndView modelAndView = new ModelAndView("event-edit");
        modelAndView.addObject("eventCreateRequest", eventCreateRequest);
        modelAndView.addObject("eventId", eventId);
        return modelAndView;
    }

    @PostMapping("/{eventId}")
    public ModelAndView updateEvent(@PathVariable UUID eventId,
                                    @Valid @ModelAttribute("eventCreateRequest") EventCreateRequest eventCreateRequest,
                                    BindingResult bindingResult,
                                    Principal principal) {
        User user = userService.getByEmail(principal.getName());

        eventService.validateSchedule(eventCreateRequest, bindingResult);

        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView("event-edit");
            modelAndView.addObject("eventCreateRequest", eventCreateRequest);
            modelAndView.addObject("eventId", eventId);
            return modelAndView;
        }

        eventService.update(eventId, eventCreateRequest, user);
        return new ModelAndView("redirect:/home?updated");
    }

    @DeleteMapping("/{eventId}")
    public ModelAndView deleteEvent(@PathVariable UUID eventId, Principal principal,
                                    @RequestParam(value = "redirect", required = false, defaultValue = "home") String redirect) {
        User user = userService.getByEmail(principal.getName());
        eventService.delete(eventId, user);
        if ("events".equals(redirect)) {
            return new ModelAndView("redirect:/events?deleted");
        }
        return new ModelAndView("redirect:/home?deleted");
    }
}

