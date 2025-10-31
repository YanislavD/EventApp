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
}
