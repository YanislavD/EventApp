package main.controller;

import jakarta.validation.Valid;
import main.model.Category;
import main.model.Event;
import main.model.Subscription;
import main.model.User;
import main.service.CategoryService;
import main.service.EventService;
import main.service.SubscriptionService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
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
    private final SubscriptionService subscriptionService;

    public EventController(EventService eventService, CategoryService categoryService, UserService userService, SubscriptionService subscriptionService) {
        this.eventService = eventService;
        this.categoryService = categoryService;
        this.userService = userService;
        this.subscriptionService = subscriptionService;
    }

    @ModelAttribute("categories")
    public List<Category> categories() {
        return categoryService.getAllActive();
    }

    @ModelAttribute("availableImages")
    public List<String> availableImages() {
        return List.of(
                "event-business.jpg",
                "event-networking.jpg",
                "event-workshop.jpg",
                "event-presentation.jpg",
                "event-gathering.jpg"
        );
    }

    @GetMapping("/new")
    public ModelAndView showCreateForm() {
        ModelAndView modelAndView = new ModelAndView("event-create");
        modelAndView.addObject("eventCreateRequest", new EventCreateRequest());
        return modelAndView;
    }

    @PostMapping
    public ModelAndView createEvent(@Valid @ModelAttribute("eventCreateRequest") EventCreateRequest eventCreateRequest,
                                    BindingResult bindingResult,
                                    Principal principal,
                                    RedirectAttributes redirectAttributes) {

        eventService.validateSchedule(eventCreateRequest, bindingResult);

        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView("event-create");
            modelAndView.addObject("eventCreateRequest", eventCreateRequest);
            return modelAndView;
        }

        User creator = userService.getByEmail(principal.getName());
        eventService.create(eventCreateRequest, creator);
        redirectAttributes.addFlashAttribute("successMessage", "✓ Успех! Събитието беше създадено успешно.");
        return new ModelAndView("redirect:/events");
    }

    @PostMapping("/{eventId}/subscriptions")
    public ModelAndView subscribeToEvent(@PathVariable UUID eventId, Principal principal) {
        User user = userService.getByEmail(principal.getName());
        boolean subscribed = eventService.subscribeUserToEvent(eventId, user);
        if (subscribed) {
            return new ModelAndView("redirect:/events");
        }
        return new ModelAndView("redirect:/events");
    }

    @DeleteMapping("/{eventId}/subscriptions")
    public ModelAndView unsubscribeFromEvent(@PathVariable UUID eventId, Principal principal,
                                             RedirectAttributes redirectAttributes) {
        User user = userService.getByEmail(principal.getName());
        boolean unsubscribed = eventService.unsubscribeUserFromEvent(eventId, user);
        if (unsubscribed) {
            redirectAttributes.addFlashAttribute("successMessage", "✓ Успех! Успешно се отписахте от събитието.");
            return new ModelAndView("redirect:/home");
        }
        return new ModelAndView("redirect:/home");
    }

    @GetMapping("/{eventId}/editform")
    public ModelAndView showEditForm(@PathVariable UUID eventId, Principal principal) {
        User user = userService.getByEmail(principal.getName());
        EventCreateRequest eventCreateRequest = eventService.buildEditRequest(eventId, user);

        ModelAndView modelAndView = new ModelAndView("event-edit");
        modelAndView.addObject("eventCreateRequest", eventCreateRequest);
        modelAndView.addObject("eventId", eventId);
        return modelAndView;
    }

    @PutMapping("/{eventId}")
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
        return new ModelAndView("redirect:/home");
    }

    @DeleteMapping("/{eventId}")
    public ModelAndView deleteEvent(@PathVariable UUID eventId, Principal principal,
                                    @RequestParam(value = "redirect", required = false, defaultValue = "home") String redirect,
                                    RedirectAttributes redirectAttributes) {
        User user = userService.getByEmail(principal.getName());
        eventService.delete(eventId, user);
        redirectAttributes.addFlashAttribute("successMessage", "✓ Успех! Събитието беше изтрито успешно.");
        if ("events".equals(redirect)) {
            return new ModelAndView("redirect:/events");
        }
        return new ModelAndView("redirect:/home");
    }

    @GetMapping("/{eventId}/participants")
    public ModelAndView showParticipants(@PathVariable UUID eventId, Principal principal) {
        User currentUser = userService.getByEmail(principal.getName());
        Event event = eventService.getById(eventId);

        // Проверка дали текущият потребител е организатор на събитието
        if (event.getCreator() == null || !event.getCreator().getId().equals(currentUser.getId())) {
            ModelAndView modelAndView = new ModelAndView("error/oops");
            modelAndView.addObject("title", "Достъпът е отказан");
            modelAndView.addObject("message", "Нямаш право да виждаш участниците на това събитие. Само организаторът на събитието може да вижда списъка с участници.");
            return modelAndView;
        }

        List<Subscription> subscriptions = subscriptionService.findByEventId(eventId);

        ModelAndView modelAndView = new ModelAndView("event-participants");
        modelAndView.addObject("event", event);
        modelAndView.addObject("subscriptions", subscriptions);
        return modelAndView;
    }
}

