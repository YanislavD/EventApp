package main.controller;

import main.model.User;
import main.service.RatingService;
import main.service.UserService;
import main.web.dto.EventRatingSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequestMapping("/ratings")
public class RatingController {

    private final RatingService ratingService;
    private final UserService userService;

    public RatingController(RatingService ratingService, UserService userService) {
        this.ratingService = ratingService;
        this.userService = userService;
    }

    @PostMapping
    public ModelAndView createRating(@RequestParam("eventId") UUID eventId,
                                     @RequestParam("score") Integer score,
                                     Principal principal,
                                     RedirectAttributes redirectAttributes) {
        User user = userService.getByEmail(principal.getName());
        ratingService.createRating(eventId, user.getId(), score);
        redirectAttributes.addFlashAttribute("successMessage", "✓ Оценката беше добавена успешно!");
        return new ModelAndView("redirect:/home");
    }


    @GetMapping("/event/{eventId}")
    @ResponseBody
    public ResponseEntity<EventRatingSummaryResponse> getRatingsForEvent(@PathVariable UUID eventId) {
        EventRatingSummaryResponse response = ratingService.getRatingsForEvent(eventId);
        return ResponseEntity.ok(response);
    }
}

