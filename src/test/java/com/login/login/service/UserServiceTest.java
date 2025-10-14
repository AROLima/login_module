package com.login.login.service;

import com.login.login.domain.User;
import com.login.login.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para UserService
 * 
 * Testa apenas os métodos que realmente existem:
 * - createUser(String email, String password, String name)  
 * - emailExists(String email)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("User Service Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .password("hashedPassword123")
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("Should create user successfully when email is available")
    void shouldCreateUserSuccessfully() {
        // Arrange
        String email = "newuser@example.com";
        String password = "plainPassword123";
        String name = "New User";
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty()); // Email disponível
        when(passwordEncoder.encode(password)).thenReturn("hashedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // Act
        User createdUser = userService.createUser(email, password, name);
        
        // Assert
        assertThat(createdUser).isNotNull();
        assertThat(createdUser).isEqualTo(testUser);
        
        // Verify interactions
        verify(userRepository).findByEmail(email); // Verifica se email existe
        verify(passwordEncoder).encode(password); // Hashe a senha
        verify(userRepository).save(any(User.class)); // Salva usuário
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        // Arrange
        String email = "existing@example.com";
        String password = "plainPassword123";  
        String name = "Test User";
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser)); // Email já existe
        
        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(email, password, name))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Email já cadastrado");
        
        // Verify interactions
        verify(userRepository).findByEmail(email);
        verifyNoInteractions(passwordEncoder); // Não deve tentar hasher senha
        verify(userRepository, never()).save(any(User.class)); // Não deve salvar
    }

    @Test
    @DisplayName("Should return true when email exists")
    void shouldReturnTrueWhenEmailExists() {
        // Arrange
        String existingEmail = "existing@example.com";
        when(userRepository.findByEmail(existingEmail)).thenReturn(Optional.of(testUser));
        
        // Act
        boolean exists = userService.emailExists(existingEmail);
        
        // Assert
        assertThat(exists).isTrue();
        verify(userRepository).findByEmail(existingEmail);
    }

    @Test
    @DisplayName("Should return false when email does not exist")
    void shouldReturnFalseWhenEmailDoesNotExist() {
        // Arrange
        String nonExistentEmail = "nonexistent@example.com";
        when(userRepository.findByEmail(nonExistentEmail)).thenReturn(Optional.empty());
        
        // Act
        boolean exists = userService.emailExists(nonExistentEmail);
        
        // Assert
        assertThat(exists).isFalse();
        verify(userRepository).findByEmail(nonExistentEmail);
    }

    @Test
    @DisplayName("Should create user with encoded password")
    void shouldCreateUserWithEncodedPassword() {
        // Arrange
        String email = "encode@example.com";
        String plainPassword = "mySecretPassword";
        String name = "Encode User";
        String hashedPassword = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        
        User expectedUser = User.builder()
                .id(2L)
                .email(email)
                .name(name)
                .password(hashedPassword)
                .enabled(true)
                .build();
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(plainPassword)).thenReturn(hashedPassword);
        when(userRepository.save(any(User.class))).thenReturn(expectedUser);
        
        // Act
        User createdUser = userService.createUser(email, plainPassword, name);
        
        // Assert
        assertThat(createdUser.getPassword()).isEqualTo(hashedPassword);
        assertThat(createdUser.getEmail()).isEqualTo(email);
        assertThat(createdUser.getName()).isEqualTo(name);
        
        // Verify password encoding happened
        verify(passwordEncoder).encode(plainPassword);
    }

    @Test
    @DisplayName("Should verify repository interactions in correct order")
    void shouldVerifyRepositoryInteractionsInCorrectOrder() {
        // Arrange
        String email = "order@example.com";
        String password = "password123";
        String name = "Order Test";
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // Act
        userService.createUser(email, password, name);
        
        // Assert - verify order of operations
        var inOrder = inOrder(userRepository, passwordEncoder);
        inOrder.verify(userRepository).findByEmail(email);     // First: check if email exists
        inOrder.verify(passwordEncoder).encode(password);      // Second: encode password  
        inOrder.verify(userRepository).save(any(User.class)); // Third: save user
    }

    @Test
    @DisplayName("Should create multiple users with different emails")
    void shouldCreateMultipleUsersWithDifferentEmails() {
        // Arrange
        String email1 = "user1@example.com";
        String email2 = "user2@example.com";
        String password = "password123";
        String hashedPassword = "hashedPassword123";
        
        User user1 = User.builder().id(1L).email(email1).name("User 1").password(hashedPassword).enabled(true).build();
        User user2 = User.builder().id(2L).email(email2).name("User 2").password(hashedPassword).enabled(true).build();
        
        when(userRepository.findByEmail(email1)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(email2)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn(hashedPassword);
        when(userRepository.save(any(User.class))).thenReturn(user1, user2);
        
        // Act
        User createdUser1 = userService.createUser(email1, password, "User 1");
        User createdUser2 = userService.createUser(email2, password, "User 2");
        
        // Assert
        assertThat(createdUser1.getEmail()).isEqualTo(email1);
        assertThat(createdUser2.getEmail()).isEqualTo(email2);
        
        verify(userRepository, times(2)).save(any(User.class));
        verify(passwordEncoder, times(2)).encode(password);
    }

    @Test
    @DisplayName("Should handle edge cases gracefully")
    void shouldHandleEdgeCases() {
        // Test different password lengths
        when(userRepository.findByEmail("short@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("abc")).thenReturn("shortHashed");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // Act & Assert - should work with short password
        assertThatCode(() -> userService.createUser("short@example.com", "abc", "Short Password User"))
            .doesNotThrowAnyException();
        
        // Test case sensitivity of email checking
        when(userRepository.findByEmail("CASE@EXAMPLE.COM")).thenReturn(Optional.empty());
        assertThat(userService.emailExists("CASE@EXAMPLE.COM")).isFalse();
    }
}