package main.service;

import jakarta.transaction.Transactional;
import main.event.UserRegisteredEvent;
import main.exception.UserAlreadyExistsException;
import main.model.Role;
import main.model.User;
import main.repository.UserRepository;
import main.web.dto.RegisterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventService eventService;
    private final SubscriptionService subscriptionService;
    private final ApplicationEventPublisher eventPublisher;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       EventService eventService,
                       SubscriptionService subscriptionService,
                       ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventService = eventService;
        this.subscriptionService = subscriptionService;
        this.eventPublisher = eventPublisher;
    }

    public Long getCount() {
        return userRepository.count();
    }

    @Transactional
    public void register(RegisterRequest registerRequest) {

        if (userRepository.findByUsername(registerRequest.getUsername()) != null || userRepository.findByEmail(registerRequest.getEmail()) != null){
            throw new UserAlreadyExistsException("Потребителското име или имейл вече е заето");
        }

        User user = User.builder()
                .username(registerRequest.getUsername())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .email(registerRequest.getEmail())
                .role(Role.USER)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new UserRegisteredEvent(this, saved));
        logger.info("User registered: {}", saved.getEmail());
    }

    public void updateNames(UUID userId, String firstName, String lastName) {
       
        String authenticatedEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User authenticatedUser = userRepository.findByEmail(authenticatedEmail);
        if (authenticatedUser == null || !authenticatedUser.getId().equals(userId)) {
            throw new AccessDeniedException("Не можеш да редактираш профила на друг потребител");
        }
        User user = userRepository.findById(userId).orElseThrow();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUpdatedOn(LocalDateTime.now());
        userRepository.save(user);
    }

    public User getByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> getAll() {
        return userRepository.findAll();
    }

    @Transactional
    public void updateRole(UUID userId, Role newRole) {
        if (userId == null) {
            throw new IllegalArgumentException("Идентификаторът на потребителя е задължителен");
        }
        if (newRole == null) {
            throw new IllegalArgumentException("Ролята е задължителна");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Потребителят не е намерен"));

        if (user.getRole() == newRole) {
            throw new IllegalStateException("Потребителят вече има тази роля");
        }

        user.setRole(newRole);
        user.setUpdatedOn(LocalDateTime.now());
        userRepository.save(user);
        logger.info("User role updated: {} to role {}", user.getEmail(), newRole.name());
    }

    @Transactional
    public void deleteUserWithData(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("Идентификаторът на потребителя е задължителен");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Потребителят не е намерен"));

        subscriptionService.deleteAllByUserId(userId);

        eventService.deleteAllByCreatorId(userId);

        userRepository.delete(user);
        logger.info("User deleted with related data: {}", user.getEmail());
    }
}
