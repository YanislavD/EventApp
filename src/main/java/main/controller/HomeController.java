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
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;

@Controller
public class HomeController {

    private final UserService userService;
    private final EventService eventService;

    public HomeController(EventService eventService, UserService userService) {
        this.userService = userService;
        this.eventService = eventService;
    }

    @GetMapping("/home")
    public ModelAndView home(Principal principal) {
        ModelAndView mv = new ModelAndView("home");

        User user = userService.getByEmail(principal.getName());
        List<EventView> subscribedEvents = eventService.getSubscribedEvents(user.getId());
        List<EventView> createdEvents = eventService.getCreatedEvents(user.getId());

        mv.addObject("subscribedEvents", subscribedEvents);
        mv.addObject("createdEvents", createdEvents);
        mv.addObject("user", user);
        return mv;
    }

    @GetMapping("/profile")
    public ModelAndView profile(Principal principal) {
        User user = userService.getByEmail(principal.getName());
        ModelAndView mv = new ModelAndView("profile");
        mv.addObject("user", user);
        return mv;
    }

    @GetMapping("/profile/edit")
    public ModelAndView editProfileForm(Principal principal) {
        User user = userService.getByEmail(principal.getName());
        ModelAndView mv = new ModelAndView("profile-edit");
        mv.addObject("user", user);
        return mv;
    }

    @PostMapping("/profile/edit")
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
        Sort sort = resolveSort(sortParam);

        ModelAndView mv = new ModelAndView("events");
        mv.addObject("events", eventService.getEventsForListing(user.getId(), sort, categoryId));
        mv.addObject("selectedSort", sortParam);
        mv.addObject("selectedCategory", categoryId);
        mv.addObject("user", user);
        mv.addObject("categories", eventService.getAvailableCategories());
        return mv;
    }

    private Sort resolveSort(String sortParam) {
        if ("startDesc".equalsIgnoreCase(sortParam)) {
            return Sort.by(Sort.Direction.DESC, "startTime");
        }
        if ("categoryAsc".equalsIgnoreCase(sortParam)) {
            return Sort.by(Sort.Direction.ASC, "category.name").and(Sort.by(Sort.Direction.ASC, "startTime"));
        }
        if ("categoryDesc".equalsIgnoreCase(sortParam)) {
            return Sort.by(Sort.Direction.DESC, "category.name").and(Sort.by(Sort.Direction.ASC, "startTime"));
        }
        return Sort.by(Sort.Direction.ASC, "startTime");
    }
}


