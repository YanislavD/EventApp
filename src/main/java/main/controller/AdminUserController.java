package main.controller;

import main.model.Role;
import main.model.User;
import main.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

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
                                       @RequestParam String role) {
        try {
            Role newRole;
            try {
                newRole = Role.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException ex) {
                return new ModelAndView("redirect:/admin/users?error=invalidRole");
            }

            userService.updateRole(userId, newRole);
            return new ModelAndView("redirect:/admin/users?success=roleChanged");
        } catch (IllegalArgumentException ex) {
            return new ModelAndView("redirect:/admin/users?error=notFound");
        } catch (IllegalStateException ex) {
            return new ModelAndView("redirect:/admin/users?error=alreadyHasRole");
        }
    }
}

