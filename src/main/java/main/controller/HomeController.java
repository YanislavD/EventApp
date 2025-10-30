package main.controller;

import main.model.Subscription;
import main.model.User;
import main.service.SubscriptionService;
import main.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.security.Principal;
import java.util.List;

@Controller
public class HomeController {

    private final SubscriptionService subscriptionService;
    private final UserService userService;

    public HomeController(SubscriptionService subscriptionService, UserService userService) {
        this.userService = userService;
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/home")
    public ModelAndView home(Principal principal) {
        ModelAndView mv = new ModelAndView("home");

        User user = userService.getByEmail(principal.getName());
        List<Subscription> subscriptions = subscriptionService.findByUserId(user.getId());

        mv.addObject("subscriptions", subscriptions);
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
    public ModelAndView events() {
        // Simple placeholder page; subscribing handled elsewhere
        return new ModelAndView("events");
    }
}


