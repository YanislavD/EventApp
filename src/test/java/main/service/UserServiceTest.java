package main.service;

import main.event.UserRegisteredEvent;
import main.exception.UserAlreadyExistsException;
import main.model.Role;
import main.model.User;
import main.repository.UserRepository;
import main.web.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EventService eventService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserService userService;

    @Test
    void whenValidRegisterRequest_thenUserIsRegistered() {
  
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByUsername("testuser")).thenReturn(null);
        when(userRepository.findByEmail("test@example.com")).thenReturn(null);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        
        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setUsername("testuser");
        savedUser.setEmail("test@example.com");
        savedUser.setPassword("encodedPassword");
        savedUser.setRole(Role.USER);
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        userService.register(request);

        verify(userRepository).findByUsername("testuser");
        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
        verify(eventPublisher).publishEvent(any(UserRegisteredEvent.class));
    }

    @Test
    void whenRegisterWithExistingUsername_thenThrowException() {
 
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        User existingUser = new User();
        existingUser.setUsername("existinguser");
        
        when(userRepository.findByUsername("existinguser")).thenReturn(existingUser);

        assertThrows(UserAlreadyExistsException.class, () -> {
            userService.register(request);
        });

        verify(userRepository).findByUsername("existinguser");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void whenRegisterWithExistingEmail_thenThrowException() {

        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("existing@example.com");
        request.setPassword("password123");

        when(userRepository.findByUsername("newuser")).thenReturn(null);
        
        User existingUser = new User();
        existingUser.setEmail("existing@example.com");
        when(userRepository.findByEmail("existing@example.com")).thenReturn(existingUser);

        assertThrows(UserAlreadyExistsException.class, () -> {
            userService.register(request);
        });

        verify(userRepository).findByUsername("newuser");
        verify(userRepository).findByEmail("existing@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void whenGetCount_thenReturnCount() {
        
        long expectedCount = 5L;
        when(userRepository.count()).thenReturn(expectedCount);

        Long result = userService.getCount();

        assertEquals(expectedCount, result);
        verify(userRepository).count();
    }

    @Test
    void whenGetByEmail_thenReturnUser() {
 
        String email = "test@example.com";
        User user = new User();
        user.setEmail(email);
        user.setUsername("testuser");
        
        when(userRepository.findByEmail(email)).thenReturn(user);

        User result = userService.getByEmail(email);

        assertNotNull(result);
        assertEquals(email, result.getEmail());
        verify(userRepository).findByEmail(email);
    }

    @Test
    void whenGetByEmailNotFound_thenReturnNull() {

        String email = "notfound@example.com";
        when(userRepository.findByEmail(email)).thenReturn(null);

        User result = userService.getByEmail(email);

        assertNull(result);
        verify(userRepository).findByEmail(email);
    }

    @Test
    void whenGetAll_thenReturnAllUsers() {

        List<User> users = new ArrayList<>();
        User user1 = new User();
        user1.setUsername("user1");
        User user2 = new User();
        user2.setUsername("user2");
        users.add(user1);
        users.add(user2);
        
        when(userRepository.findAll()).thenReturn(users);

        List<User> result = userService.getAll();

        assertEquals(2, result.size());
        verify(userRepository).findAll();
    }

    @Test
    void whenUpdateRoleWithValidInput_thenUpdateRole() {
  
        UUID userId = UUID.randomUUID();
        Role newRole = Role.ADMIN;

        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setRole(Role.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.updateRole(userId, newRole);

        assertEquals(newRole, user.getRole());
        verify(userRepository).findById(userId);
        verify(userRepository).save(user);
    }

    @Test
    void whenUpdateRoleWithNullUserId_thenThrowException() {

        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateRole(null, Role.ADMIN);
        });

        verify(userRepository, never()).findById(any());
    }

    @Test
    void whenUpdateRoleWithNullRole_thenThrowException() {

        UUID userId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateRole(userId, null);
        });

        verify(userRepository, never()).findById(any());
    }

    @Test
    void whenUpdateRoleWithUserNotFound_thenThrowException() {
   
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateRole(userId, Role.ADMIN);
        });

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
    }

    @Test
    void whenUpdateRoleWithSameRole_thenThrowException() {

        UUID userId = UUID.randomUUID();
        Role currentRole = Role.USER;

        User user = new User();
        user.setId(userId);
        user.setRole(currentRole);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(IllegalStateException.class, () -> {
            userService.updateRole(userId, currentRole);
        });

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
    }

    @Test
    void whenDeleteUserWithData_thenDeleteUser() {

        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.deleteUserWithData(userId);

        verify(subscriptionService).deleteAllByUserId(userId);
        verify(eventService).deleteAllByCreatorId(userId);
        verify(userRepository).delete(user);
    }

    @Test
    void whenDeleteUserWithNullId_thenThrowException() {

        assertThrows(IllegalArgumentException.class, () -> {
            userService.deleteUserWithData(null);
        });

        verify(userRepository, never()).findById(any());
    }

    @Test
    void whenDeleteUserNotFound_thenThrowException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            userService.deleteUserWithData(userId);
        });

        verify(userRepository).findById(userId);
        verify(userRepository, never()).delete(any());
    }
}
