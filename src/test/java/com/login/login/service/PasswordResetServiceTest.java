package com.login.login.service;

import com.login.login.domain.PasswordResetToken;
import com.login.login.domain.User;
import com.login.login.mail.MailService;
import com.login.login.repo.PasswordResetTokenRepository;
import com.login.login.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para PasswordResetService
 * 
 * Testa apenas os métodos que realmente existem:
 * - request(String email)
 * - reset(String token, String newPassword)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Password Reset Service Tests")
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MailService mailService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User testUser;
    private PasswordResetToken testToken;

    @BeforeEach
    void setUp() {
        // Configurar TTL do token via reflection (simula @Value)
        ReflectionTestUtils.setField(passwordResetService, "ttl", 30L);

        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .password("currentHashedPassword")
                .enabled(true)
                .build();

        testToken = PasswordResetToken.builder()
                .id(1L)
                .token("valid-token-123")
                .user(testUser)
                .expiresAt(Instant.now().plusSeconds(1800)) // 30 minutos no futuro
                .used(false)
                .build();
    }

    @Test
    @DisplayName("Should create reset request successfully when user exists")
    void shouldCreateResetRequestSuccessfully() {
        // Arrange
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenReturn(testToken);

        // Act
        passwordResetService.request(email);

        // Assert
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        
        PasswordResetToken capturedToken = tokenCaptor.getValue();
        assertThat(capturedToken.getUser()).isEqualTo(testUser);
        assertThat(capturedToken.getToken()).isNotNull();
        assertThat(capturedToken.getExpiresAt()).isAfter(Instant.now());
        assertThat(capturedToken.isUsed()).isFalse();

        // Verify email was sent
        verify(mailService).sendResetEmail(eq(email), any(String.class));
    }

    @Test
    @DisplayName("Should silently ignore reset request when user does not exist")
    void shouldSilentlyIgnoreResetRequestWhenUserNotExists() {
        // Arrange
        String nonExistentEmail = "nonexistent@example.com";
        when(userRepository.findByEmail(nonExistentEmail)).thenReturn(Optional.empty());

        // Act
        passwordResetService.request(nonExistentEmail);

        // Assert - should not create token or send email for non-existent user
        verify(tokenRepository, never()).save(any(PasswordResetToken.class));
        verify(mailService, never()).sendResetEmail(any(String.class), any(String.class));
    }

    @Test
    @DisplayName("Should reset password successfully with valid token")
    void shouldResetPasswordSuccessfullyWithValidToken() {
        // Arrange
        String token = "valid-token-123";
        String newPassword = "newSecretPassword";
        String hashedNewPassword = "hashedNewSecretPassword";
        
        when(tokenRepository.findByTokenAndUsedFalse(token)).thenReturn(Optional.of(testToken));
        when(passwordEncoder.encode(newPassword)).thenReturn(hashedNewPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(tokenRepository.save(any(PasswordResetToken.class))).thenReturn(testToken);

        // Act
        passwordResetService.reset(token, newPassword);

        // Assert
        // Verify user password was updated
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        
        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getPassword()).isEqualTo(hashedNewPassword);

        // Verify token was marked as used
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        
        PasswordResetToken capturedToken = tokenCaptor.getValue();
        assertThat(capturedToken.isUsed()).isTrue();

        // Verify password encoding
        verify(passwordEncoder).encode(newPassword);
    }

    @Test
    @DisplayName("Should throw exception when token does not exist")
    void shouldThrowExceptionWhenTokenDoesNotExist() {
        // Arrange
        String invalidToken = "invalid-token";
        String newPassword = "newPassword";
        
        when(tokenRepository.findByTokenAndUsedFalse(invalidToken)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> passwordResetService.reset(invalidToken, newPassword))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid token");

        // Verify no password changes or token updates
        verify(passwordEncoder, never()).encode(any(String.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when token is expired")
    void shouldThrowExceptionWhenTokenIsExpired() {
        // Arrange
        String expiredTokenValue = "expired-token";
        String newPassword = "newPassword";
        
        PasswordResetToken expiredToken = PasswordResetToken.builder()
                .id(2L)
                .token(expiredTokenValue)
                .user(testUser)
                .expiresAt(Instant.now().minusSeconds(3600)) // 1 hora no passado
                .used(false)
                .build();
        
        when(tokenRepository.findByTokenAndUsedFalse(expiredTokenValue)).thenReturn(Optional.of(expiredToken));

        // Act & Assert
        assertThatThrownBy(() -> passwordResetService.reset(expiredTokenValue, newPassword))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Token expired");

        // Verify no password changes
        verify(passwordEncoder, never()).encode(any(String.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should generate unique tokens for different requests")
    void shouldGenerateUniqueTokensForDifferentRequests() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenReturn(testToken);

        // Act - make multiple requests
        passwordResetService.request("test@example.com");
        passwordResetService.request("test@example.com");

        // Assert - verify unique tokens were generated
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository, times(2)).save(tokenCaptor.capture());
        
        var capturedTokens = tokenCaptor.getAllValues();
        assertThat(capturedTokens.get(0).getToken()).isNotEqualTo(capturedTokens.get(1).getToken());

        // Verify emails were sent for both requests
        verify(mailService, times(2)).sendResetEmail(eq("test@example.com"), any(String.class));
    }

    @Test
    @DisplayName("Should handle token that expired one second ago")
    void shouldHandleTokenExpiredOneSecondAgo() {
        // Arrange
        String tokenValue = "exactly-expired-token";
        String newPassword = "newPassword";
        
        PasswordResetToken exactlyExpiredToken = PasswordResetToken.builder()
                .id(3L)
                .token(tokenValue)
                .user(testUser)
                .expiresAt(Instant.now().minusSeconds(1)) // 1 segundo no passado
                .used(false)
                .build();
        
        when(tokenRepository.findByTokenAndUsedFalse(tokenValue)).thenReturn(Optional.of(exactlyExpiredToken));

        // Act & Assert - should fail because token expired 1 second ago
        assertThatThrownBy(() -> passwordResetService.reset(tokenValue, newPassword))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Token expired");
    }

    @Test
    @DisplayName("Should verify correct interaction order during reset")
    void shouldVerifyCorrectInteractionOrderDuringReset() {
        // Arrange
        String token = "valid-token";
        String newPassword = "newPassword";
        
        when(tokenRepository.findByTokenAndUsedFalse(token)).thenReturn(Optional.of(testToken));
        when(passwordEncoder.encode(newPassword)).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(tokenRepository.save(any(PasswordResetToken.class))).thenReturn(testToken);

        // Act
        passwordResetService.reset(token, newPassword);

        // Assert - verify order of operations
        var inOrder = inOrder(tokenRepository, passwordEncoder, userRepository);
        inOrder.verify(tokenRepository).findByTokenAndUsedFalse(token);     // First: find token
        inOrder.verify(passwordEncoder).encode(newPassword);                // Second: encode new password
        inOrder.verify(userRepository).save(any(User.class));              // Third: save user with new password
        inOrder.verify(tokenRepository).save(any(PasswordResetToken.class)); // Fourth: mark token as used
    }

    @Test
    @DisplayName("Should handle multiple reset attempts with same token")
    void shouldHandleMultipleResetAttemptsWithSameToken() {
        // Arrange
        String token = "single-use-token";
        String password1 = "firstPassword";
        String password2 = "secondPassword";
        
        // First call returns token, second call returns empty (token already used)
        when(tokenRepository.findByTokenAndUsedFalse(token))
            .thenReturn(Optional.of(testToken))
            .thenReturn(Optional.empty());
        
        when(passwordEncoder.encode(password1)).thenReturn("hashedFirstPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(tokenRepository.save(any(PasswordResetToken.class))).thenReturn(testToken);

        // Act & Assert
        // First reset should succeed
        assertThatCode(() -> passwordResetService.reset(token, password1))
            .doesNotThrowAnyException();

        // Second reset with same token should fail
        assertThatThrownBy(() -> passwordResetService.reset(token, password2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid token");

        // Verify only one password change occurred
        verify(passwordEncoder, times(1)).encode(any(String.class));
        verify(userRepository, times(1)).save(any(User.class));
    }
}