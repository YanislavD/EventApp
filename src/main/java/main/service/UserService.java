package main.service;

import jakarta.transaction.Transactional;
import main.exception.UserAlreadyExistsException;
import main.model.Role;
import main.model.User;
import main.repository.UserRepository;
import main.web.dto.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Long getCount() {
        return userRepository.count();
    }

    @Transactional
    public void register(RegisterRequest registerRequest) {

        if (userRepository.findByUsername(registerRequest.getUsername()) != null || userRepository.findByEmail(registerRequest.getEmail()) != null){
            throw new UserAlreadyExistsException("Username or Email already exists");
        }

        User user = User.builder()
                .username(registerRequest.getUsername())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .email(registerRequest.getEmail())
                .role(Role.USER)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        userRepository.save(user);
    }

    public void updateNames(UUID userId, String firstName, String lastName) {
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

    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Потребителят не е намерен"));
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
    }
}
