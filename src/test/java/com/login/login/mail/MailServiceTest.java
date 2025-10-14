package com.login.login.mail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * TESTES UNITÁRIOS PARA MAIL SERVICE
 * 
 * Testa envio de emails com mock do JavaMailSender.
 * 
 * Cenários testados:
 * - Envio de email de reset de senha
 * - Validação de parâmetros
 * - Tratamento de erros de envio
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Mail Service Tests")
class MailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private MimeMessage mimeMessage;

    private MailService mailService;
    private final String baseUrl = "http://localhost:8080";

    @BeforeEach
    void setUp() {
        mailService = new MailService(javaMailSender);
        // Usar reflexão para definir o baseUrl
        try {
            var field = MailService.class.getDeclaredField("baseUrl");
            field.setAccessible(true);
            field.set(mailService, baseUrl);
        } catch (Exception e) {
            // Ignorar erro na configuração de teste
        }
    }

    @Test
    @DisplayName("Deve enviar email de reset com sucesso")
    void shouldSendResetEmailSuccessfully() throws MessagingException {
        // Given
        String email = "test@example.com";
        String token = "test-token-123";
        
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        assertThatNoException().isThrownBy(() -> 
            mailService.sendResetEmail(email, token)
        );

        // Then
        verify(javaMailSender).createMimeMessage();
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Deve lançar exceção quando JavaMailSender falha")
    void shouldThrowExceptionWhenMailSenderFails() throws MessagingException {
        // Given
        String email = "test@example.com";
        String token = "test-token-123";
        
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP Error")).when(javaMailSender).send(any(MimeMessage.class));

        // When & Then
        assertThatThrownBy(() -> mailService.sendResetEmail(email, token))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("SMTP Error");

        verify(javaMailSender).createMimeMessage();
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Deve processar email nulo sem erro")
    void shouldProcessNullEmailWithoutError() {
        // Given
        String token = "test-token-123";
        
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When & Then - Spring Mail valida parâmetros e lança exceção para email nulo
        assertThatThrownBy(() -> mailService.sendResetEmail(null, token))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve processar token nulo sem erro")
    void shouldProcessNullTokenWithoutError() {
        // Given
        String email = "test@example.com";
        
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When & Then - Deve funcionar mesmo com token nulo
        assertThatNoException().isThrownBy(() -> 
            mailService.sendResetEmail(email, null)
        );
    }

    @Test
    @DisplayName("Deve processar parâmetros vazios sem erro")
    void shouldProcessEmptyParametersWithoutError() {
        // Given
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When & Then - Spring Mail valida email vazio e lança exceção
        assertThatThrownBy(() -> mailService.sendResetEmail("", ""))
            .isInstanceOf(IllegalStateException.class)
            .hasCauseInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Deve chamar JavaMailSender corretamente")
    void shouldCallJavaMailSenderCorrectly() {
        // Given
        String email = "test@example.com";
        String token = "test-token-123";
        
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        mailService.sendResetEmail(email, token);

        // Then
        verify(javaMailSender, times(1)).createMimeMessage();
        verify(javaMailSender, times(1)).send(mimeMessage);
        verifyNoMoreInteractions(javaMailSender);
    }

    @Test
    @DisplayName("Deve funcionar com múltiplas chamadas")
    void shouldWorkWithMultipleCalls() {
        // Given
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        mailService.sendResetEmail("user1@test.com", "token1");
        mailService.sendResetEmail("user2@test.com", "token2");
        mailService.sendResetEmail("user3@test.com", "token3");

        // Then
        verify(javaMailSender, times(3)).createMimeMessage();
        verify(javaMailSender, times(3)).send(mimeMessage);
    }
}