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
    public ModelAndView events(Principal principal) {
        User user = userService.getByEmail(principal.getName());
        ModelAndView mv = new ModelAndView("events");
        mv.addObject("events", eventService.getEventsForListing(user.getId()));
        mv.addObject("user", user);
        return mv;
    }
}


