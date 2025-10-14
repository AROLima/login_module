package com.login.login.repo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import com.login.login.domain.PasswordResetToken;
import com.login.login.domain.User;

/**
 * TESTES DE INTEGRAÇÃO PARA PASSWORD RESET TOKEN REPOSITORY
 * 
 * Testa operações de banco de dados para tokens de reset de senha.
 * 
 * @DataJpaTest configura:
 * - Banco H2 em memória
 * - Apenas beans relacionados a JPA
 * - Transações com rollback automático
 */
@DataJpaTest
@DisplayName("Password Reset Token Repository Tests")
class PasswordResetTokenRepositoryTest {

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private PasswordResetToken testToken;

    @BeforeEach
    void setUp() {
        // Criar usuário de teste
        testUser = User.builder()
            .email("test@example.com")
            .name("Test User")
            .password("password123")
            .build();
        
        testUser = entityManager.persistAndFlush(testUser);

        // Criar token de teste
        testToken = PasswordResetToken.builder()
            .token(UUID.randomUUID().toString())
            .user(testUser)
            .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
            .used(false)
            .build();
    }

    @Test
    @DisplayName("Deve salvar token de reset com sucesso")
    void shouldSavePasswordResetTokenSuccessfully() {
        // When
        PasswordResetToken savedToken = passwordResetTokenRepository.save(testToken);

        // Then
        assertThat(savedToken.getId()).isNotNull();
        assertThat(savedToken.getToken()).isEqualTo(testToken.getToken());
        assertThat(savedToken.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(savedToken.getExpiresAt()).isEqualTo(testToken.getExpiresAt());
        assertThat(savedToken.isUsed()).isFalse();
    }

    @Test
    @DisplayName("Deve encontrar token por valor")
    void shouldFindTokenByValue() {
        // Given
        passwordResetTokenRepository.save(testToken);
        entityManager.flush();

        // When
        Optional<PasswordResetToken> found = passwordResetTokenRepository.findByToken(testToken.getToken());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getToken()).isEqualTo(testToken.getToken());
        assertThat(found.get().getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("Deve encontrar token não usado")
    void shouldFindUnusedToken() {
        // Given
        passwordResetTokenRepository.save(testToken);
        entityManager.flush();

        // When
        Optional<PasswordResetToken> found = passwordResetTokenRepository.findByTokenAndUsedFalse(testToken.getToken());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().isUsed()).isFalse();
    }

    @Test
    @DisplayName("Não deve encontrar token usado")
    void shouldNotFindUsedToken() {
        // Given
        testToken.setUsed(true);
        passwordResetTokenRepository.save(testToken);
        entityManager.flush();

        // When
        Optional<PasswordResetToken> found = passwordResetTokenRepository.findByTokenAndUsedFalse(testToken.getToken());

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar vazio para token inexistente")
    void shouldReturnEmptyForNonExistentToken() {
        // When
        Optional<PasswordResetToken> found = passwordResetTokenRepository.findByToken("non-existent-token");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Deve garantir unicidade do token")
    void shouldEnforceTokenUniqueness() {
        // Given
        passwordResetTokenRepository.save(testToken);
        entityManager.flush();

        PasswordResetToken duplicateToken = PasswordResetToken.builder()
            .token(testToken.getToken()) // Mesmo token
            .user(testUser)
            .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
            .used(false)
            .build();

        // When & Then
        assertThatThrownBy(() -> {
            passwordResetTokenRepository.save(duplicateToken);
            entityManager.flush();
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Deve permitir múltiplos tokens para o mesmo usuário")
    void shouldAllowMultipleTokensForSameUser() {
        // Given
        PasswordResetToken anotherToken = PasswordResetToken.builder()
            .token(UUID.randomUUID().toString())
            .user(testUser)
            .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
            .used(false)
            .build();

        // When
        passwordResetTokenRepository.save(testToken);
        passwordResetTokenRepository.save(anotherToken);
        entityManager.flush();

        // Then
        assertThat(passwordResetTokenRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("Deve manter integridade referencial com usuário")
    void shouldMaintainReferentialIntegrityWithUser() {
        // Given
        passwordResetTokenRepository.save(testToken);
        entityManager.flush();

        // When
        Optional<PasswordResetToken> found = passwordResetTokenRepository.findByToken(testToken.getToken());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUser()).isNotNull();
        assertThat(found.get().getUser().getEmail()).isEqualTo(testUser.getEmail());
    }

    @Test
    @DisplayName("Deve excluir token com sucesso")
    void shouldDeleteTokenSuccessfully() {
        // Given
        passwordResetTokenRepository.save(testToken);
        entityManager.flush();
        
        Long tokenId = testToken.getId();

        // When
        passwordResetTokenRepository.deleteById(tokenId);
        entityManager.flush();

        // Then
        assertThat(passwordResetTokenRepository.findById(tokenId)).isEmpty();
    }

    @Test
    @DisplayName("Deve contar tokens corretamente")
    void shouldCountTokensCorrectly() {
        // Given
        passwordResetTokenRepository.save(testToken);
        
        PasswordResetToken anotherToken = PasswordResetToken.builder()
            .token(UUID.randomUUID().toString())
            .user(testUser)
            .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
            .used(false)
            .build();
        
        passwordResetTokenRepository.save(anotherToken);
        entityManager.flush();

        // When
        long count = passwordResetTokenRepository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve verificar existência do token")
    void shouldCheckTokenExistence() {
        // Given
        passwordResetTokenRepository.save(testToken);
        entityManager.flush();
        
        Long tokenId = testToken.getId();

        // When & Then
        assertThat(passwordResetTokenRepository.existsById(tokenId)).isTrue();
        assertThat(passwordResetTokenRepository.existsById(999L)).isFalse();
    }
}