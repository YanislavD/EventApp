package main.controller;

import main.model.User;
import main.service.EventService;
import main.service.UserService;
import main.web.view.EventView;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.security.Principal;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.UUID;

@Controller
public class HomeController {

    private final UserService userService;
    private final EventService eventService;

    public HomeController(UserService userService, EventService eventService) {
        this.userService = userService;
        this.eventService = eventService;
    }

    @GetMapping("/home")
    public ModelAndView home(Principal principal) {
        ModelAndView modelAndView = new ModelAndView("home");
        User user = userService.getByEmail(principal.getName());
        List<EventView> subscribedEvents = eventService.getSubscribedEvents(user.getId());
        List<EventView> createdEvents = eventService.getCreatedEvents(user.getId());
        modelAndView.addObject("subscribedEvents", subscribedEvents);
        modelAndView.addObject("createdEvents", createdEvents);
        modelAndView.addObject("user", user);
        return modelAndView;
    }

    @GetMapping("/profile")
    public ModelAndView profile(@RequestParam(value = "edit", required = false) Boolean edit,
                                Principal principal) {
        boolean editMode = Boolean.TRUE.equals(edit);
        User user = userService.getByEmail(principal.getName());
        String viewName = editMode ? "profile-edit" : "profile";
        ModelAndView modelAndView = new ModelAndView(viewName);
        modelAndView.addObject("user", user);
        return modelAndView;
    }

    @PostMapping("/profile")
    public ModelAndView editProfile(@RequestParam(required = false) String firstName,
                                    @RequestParam(required = false) String lastName,
                                    Principal principal) {
        User user = userService.getByEmail(principal.getName());
        userService.updateNames(user.getId(), firstName, lastName);
        return new ModelAndView("redirect:/profile");
    }

    @GetMapping("/events")
    public ModelAndView events(@RequestParam(value = "sort", required = false) String sortParam,
                               @RequestParam(value = "category", required = false) UUID categoryId,
                               Principal principal) {
        User user = userService.getByEmail(principal.getName());
        Sort sort = eventService.resolveSort(sortParam);
        ModelAndView modelAndView = new ModelAndView("events");
        modelAndView.addObject("events", eventService.getEventsForListing(user.getId(), sort, categoryId));
        modelAndView.addObject("selectedSort", sortParam);
        modelAndView.addObject("selectedCategory", categoryId);
        modelAndView.addObject("user", user);
        modelAndView.addObject("categories", eventService.getAvailableCategories());
        return modelAndView;
    }
}


