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

import com.login.login.domain.RefreshToken;
import com.login.login.domain.User;

/**
 * TESTES DE INTEGRAÇÃO PARA REFRESH TOKEN REPOSITORY
 * 
 * Testa operações de banco de dados para tokens de refresh.
 * 
 * @DataJpaTest configura:
 * - Banco H2 em memória
 * - Apenas beans relacionados a JPA
 * - Transações com rollback automático
 */
@DataJpaTest
@DisplayName("Refresh Token Repository Tests")
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private RefreshToken testToken;

    @BeforeEach
    void setUp() {
        // Criar usuário de teste
        testUser = User.builder()
            .email("test@example.com")
            .name("Test User")
            .password("password123")
            .build();
        
        testUser = entityManager.persistAndFlush(testUser);

        // Criar refresh token de teste
        testToken = RefreshToken.builder()
            .user(testUser)
            .tokenHash("hash123456")
            .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
            .revoked(false)
            .build();
    }

    @Test
    @DisplayName("Deve salvar refresh token com sucesso")
    void shouldSaveRefreshTokenSuccessfully() {
        // When
        RefreshToken savedToken = refreshTokenRepository.save(testToken);

        // Then
        assertThat(savedToken.getId()).isNotNull();
        assertThat(savedToken.getTokenHash()).isEqualTo(testToken.getTokenHash());
        assertThat(savedToken.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(savedToken.getExpiresAt()).isEqualTo(testToken.getExpiresAt());
        assertThat(savedToken.isRevoked()).isFalse();
    }

    @Test
    @DisplayName("Deve encontrar token por hash")
    void shouldFindTokenByHash() {
        // Given
        refreshTokenRepository.save(testToken);
        entityManager.flush();

        // When
        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash(testToken.getTokenHash());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getTokenHash()).isEqualTo(testToken.getTokenHash());
        assertThat(found.get().getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("Deve retornar vazio para hash inexistente")
    void shouldReturnEmptyForNonExistentHash() {
        // When
        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash("non-existent-hash");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Deve excluir token por hash")
    void shouldDeleteTokenByHash() {
        // Given
        refreshTokenRepository.save(testToken);
        entityManager.flush();
        
        String tokenHash = testToken.getTokenHash();

        // When
        refreshTokenRepository.deleteByTokenHash(tokenHash);
        entityManager.flush();

        // Then
        assertThat(refreshTokenRepository.findByTokenHash(tokenHash)).isEmpty();
    }

    @Test
    @DisplayName("Deve garantir unicidade do hash")
    void shouldEnforceHashUniqueness() {
        // Given
        refreshTokenRepository.save(testToken);
        entityManager.flush();

        RefreshToken duplicateToken = RefreshToken.builder()
            .user(testUser)
            .tokenHash(testToken.getTokenHash()) // Mesmo hash
            .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
            .revoked(false)
            .build();

        // When & Then
        assertThatThrownBy(() -> {
            refreshTokenRepository.save(duplicateToken);
            entityManager.flush();
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Deve permitir múltiplos tokens para o mesmo usuário")
    void shouldAllowMultipleTokensForSameUser() {
        // Given
        RefreshToken anotherToken = RefreshToken.builder()
            .user(testUser)
            .tokenHash("different-hash-456")
            .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
            .revoked(false)
            .build();

        // When
        refreshTokenRepository.save(testToken);
        refreshTokenRepository.save(anotherToken);
        entityManager.flush();

        // Then
        assertThat(refreshTokenRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("Deve manter integridade referencial com usuário")
    void shouldMaintainReferentialIntegrityWithUser() {
        // Given
        refreshTokenRepository.save(testToken);
        entityManager.flush();

        // When
        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash(testToken.getTokenHash());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUser()).isNotNull();
        assertThat(found.get().getUser().getEmail()).isEqualTo(testUser.getEmail());
    }

    @Test
    @DisplayName("Deve salvar token revogado")
    void shouldSaveRevokedToken() {
        // Given
        testToken.setRevoked(true);

        // When
        RefreshToken savedToken = refreshTokenRepository.save(testToken);
        entityManager.flush();

        // Then
        assertThat(savedToken.isRevoked()).isTrue();
        
        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash(testToken.getTokenHash());
        assertThat(found).isPresent();
        assertThat(found.get().isRevoked()).isTrue();
    }

    @Test
    @DisplayName("Deve excluir token com sucesso")
    void shouldDeleteTokenSuccessfully() {
        // Given
        refreshTokenRepository.save(testToken);
        entityManager.flush();
        
        Long tokenId = testToken.getId();

        // When
        refreshTokenRepository.deleteById(tokenId);
        entityManager.flush();

        // Then
        assertThat(refreshTokenRepository.findById(tokenId)).isEmpty();
    }

    @Test
    @DisplayName("Deve contar tokens corretamente")
    void shouldCountTokensCorrectly() {
        // Given
        refreshTokenRepository.save(testToken);
        
        RefreshToken anotherToken = RefreshToken.builder()
            .user(testUser)
            .tokenHash("another-hash-789")
            .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
            .revoked(false)
            .build();
        
        refreshTokenRepository.save(anotherToken);
        entityManager.flush();

        // When
        long count = refreshTokenRepository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve verificar existência do token")
    void shouldCheckTokenExistence() {
        // Given
        refreshTokenRepository.save(testToken);
        entityManager.flush();
        
        Long tokenId = testToken.getId();

        // When & Then
        assertThat(refreshTokenRepository.existsById(tokenId)).isTrue();
        assertThat(refreshTokenRepository.existsById(999L)).isFalse();
    }

    @Test
    @DisplayName("Deve trabalhar com diferentes usuários")
    void shouldWorkWithDifferentUsers() {
        // Given
        User anotherUser = User.builder()
            .email("another@example.com")
            .name("Another User")
            .password("password456")
            .build();
        anotherUser = entityManager.persistAndFlush(anotherUser);

        RefreshToken tokenForAnotherUser = RefreshToken.builder()
            .user(anotherUser)
            .tokenHash("hash-for-another-user")
            .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
            .revoked(false)
            .build();

        // When
        refreshTokenRepository.save(testToken);
        refreshTokenRepository.save(tokenForAnotherUser);
        entityManager.flush();

        // Then
        assertThat(refreshTokenRepository.findAll()).hasSize(2);
        
        Optional<RefreshToken> token1 = refreshTokenRepository.findByTokenHash(testToken.getTokenHash());
        Optional<RefreshToken> token2 = refreshTokenRepository.findByTokenHash(tokenForAnotherUser.getTokenHash());
        
        assertThat(token1).isPresent();
        assertThat(token2).isPresent();
        assertThat(token1.get().getUser().getId()).isEqualTo(testUser.getId());
        assertThat(token2.get().getUser().getId()).isEqualTo(anotherUser.getId());
    }
}