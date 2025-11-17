package main.controller;

import main.model.User;
import main.service.EventService;
import main.service.RatingService;
import main.service.UserService;
import main.web.dto.EventRatingSummaryResponse;
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
    private final RatingService ratingService;

    public HomeController(UserService userService, EventService eventService, RatingService ratingService) {
        this.userService = userService;
        this.eventService = eventService;
        this.ratingService = ratingService;
    }

    @GetMapping("/home")
    public ModelAndView home(Principal principal) {
        ModelAndView modelAndView = new ModelAndView("home");
        User user = userService.getByEmail(principal.getName());
        List<EventView> subscribedEvents = eventService.getSubscribedEvents(user.getId());
        List<EventView> createdEvents = eventService.getCreatedEvents(user.getId());
        List<EventView> pastSubscribedEvents = eventService.getPastSubscribedEvents(user.getId());
        List<EventView> pastCreatedEvents = eventService.getPastCreatedEvents(user.getId());
        
        java.util.Map<java.util.UUID, EventRatingSummaryResponse> ratingsMap = new java.util.HashMap<>();
        java.util.Map<java.util.UUID, Boolean> hasRatedMap = new java.util.HashMap<>();
        
        for (EventView event : pastSubscribedEvents) {
            EventRatingSummaryResponse ratingSummary = ratingService.getRatingsForEvent(event.getId());
            ratingsMap.put(event.getId(), ratingSummary);
            hasRatedMap.put(event.getId(), ratingService.hasUserRated(event.getId(), user.getId()));
        }
        
        modelAndView.addObject("subscribedEvents", subscribedEvents);
        modelAndView.addObject("createdEvents", createdEvents);
        modelAndView.addObject("pastSubscribedEvents", pastSubscribedEvents);
        modelAndView.addObject("pastCreatedEvents", pastCreatedEvents);
        modelAndView.addObject("user", user);
        modelAndView.addObject("ratingsMap", ratingsMap);
        modelAndView.addObject("hasRatedMap", hasRatedMap);
        return modelAndView;
    }

    @GetMapping("/profile")
    public ModelAndView profile(Principal principal) {
        User user = userService.getByEmail(principal.getName());
        ModelAndView modelAndView = new ModelAndView("profile");
        modelAndView.addObject("user", user);
        return modelAndView;
    }

    @GetMapping("/profile/{id}")
    public ModelAndView editProfilePage(@org.springframework.web.bind.annotation.PathVariable UUID id,
                                        Principal principal) {
        User currentUser = userService.getByEmail(principal.getName());
        if (!currentUser.getId().equals(id)) {
            return new ModelAndView("redirect:/profile");
        }
        ModelAndView modelAndView = new ModelAndView("profile-edit");
        modelAndView.addObject("user", currentUser);
        return modelAndView;
    }

    @PostMapping("/profile/{id}")
    public ModelAndView editProfile(@org.springframework.web.bind.annotation.PathVariable UUID id,
                                    @RequestParam(required = false) String firstName,
                                    @RequestParam(required = false) String lastName,
                                    Principal principal) {
        userService.updateNames(id, firstName, lastName);
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
        modelAndView.addObject("pastEvents", eventService.getPastEventsForListing(user.getId(), sort, categoryId));
        modelAndView.addObject("selectedSort", sortParam);
        modelAndView.addObject("selectedCategory", categoryId);
        modelAndView.addObject("user", user);
        modelAndView.addObject("categories", eventService.getAvailableCategories());
        return modelAndView;
    }
}


