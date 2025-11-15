package main.controller;

import main.model.Role;
import main.model.User;
import main.service.UserService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ModelAndView showUsers() {
        ModelAndView modelAndView = new ModelAndView("admin-users");
        List<User> users = userService.getAll();
        modelAndView.addObject("users", users);
        modelAndView.addObject("roles", Role.values());
        return modelAndView;
    }

    @PostMapping("/{userId}/role")
    public ModelAndView changeUserRole(@PathVariable UUID userId,
                                       @RequestParam String role,
                                       Principal principal,
                                       RedirectAttributes redirectAttributes) {
        Role newRole = Role.valueOf(role.toUpperCase());

        User currentUser = userService.getByEmail(principal.getName());
        boolean changingOwnRole = currentUser.getId().equals(userId);

        userService.updateRole(userId, newRole);

        if (changingOwnRole) {
            SecurityContextHolder.clearContext();
            return new ModelAndView("redirect:/logout");
        }

        redirectAttributes.addFlashAttribute("successMessage", "Ролята беше променена успешно.");
        return new ModelAndView("redirect:/admin/users");
    }

    @PostMapping("/{userId}/delete")
    public ModelAndView deleteUser(@PathVariable UUID userId,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        User currentUser = userService.getByEmail(principal.getName());
        if (currentUser.getId().equals(userId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Не можеш да изтриеш своя профил.");
            return new ModelAndView("redirect:/admin/users");
        }

        try {
            userService.deleteUserWithData(userId);
            redirectAttributes.addFlashAttribute("successMessage", "Потребителят беше изтрит успешно.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return new ModelAndView("redirect:/admin/users");
    }
}

