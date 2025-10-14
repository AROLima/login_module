package com.login.login.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;


import com.login.login.domain.User;
import io.jsonwebtoken.JwtException;

/**
 * TESTES UNITÁRIOS PARA JWT SERVICE
 * 
 * Testa criação e validação de tokens JWT.
 * 
 * Cenários testados:
 * - Criação de access tokens válidos
 * - Extração de user ID dos tokens
 * - Validação de tokens com diferentes cenários
 * - Tratamento de tokens inválidos
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Service Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Configurar JwtService com parâmetros de teste
        String testSecret = "dGVzdFNlY3JldEtleTEyMzQ1Njc4OTBhYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5eg==";
        String testIssuer = "test-app";
        Long testTtl = 30L; // 30 minutos
        
        jwtService = new JwtService(testSecret, testIssuer, testTtl);
        
        // Criar usuário de teste
        testUser = User.builder()
            .id(1L)
            .email("test@example.com")
            .name("Test User")
            .build();
    }

    @Test
    @DisplayName("Deve criar access token com sucesso")
    void shouldCreateAccessTokenSuccessfully() {
        // When
        String token = jwtService.createAcessToken(testUser);

        // Then
        assertThat(token).isNotBlank();
        assertThat(token).contains(".");
        assertThat(token.split("\\.")).hasSize(3); // Header.Payload.Signature
    }

    @Test
    @DisplayName("Deve extrair user ID do token corretamente")
    void shouldExtractUserIdFromToken() {
        // Given
        String token = jwtService.createAcessToken(testUser);

        // When
        Long userId = jwtService.subjectToUserId(token);

        // Then
        assertThat(userId).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("Deve falhar ao extrair ID de token inválido")
    void shouldFailToExtractIdFromInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        // When & Then
        assertThatThrownBy(() -> jwtService.subjectToUserId(invalidToken))
            .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Deve falhar ao extrair ID de token nulo")
    void shouldFailToExtractIdFromNullToken() {
        // When & Then
        assertThatThrownBy(() -> jwtService.subjectToUserId(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve falhar ao extrair ID de token vazio")
    void shouldFailToExtractIdFromEmptyToken() {
        // When & Then
        assertThatThrownBy(() -> jwtService.subjectToUserId(""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve criar tokens únicos para diferentes usuários")
    void shouldCreateUniqueTokensForDifferentUsers() {
        // Given
        User anotherUser = User.builder()
            .id(2L)
            .email("another@example.com")
            .name("Another User")
            .build();

        // When
        String token1 = jwtService.createAcessToken(testUser);
        String token2 = jwtService.createAcessToken(anotherUser);

        // Then
        assertThat(token1).isNotEqualTo(token2);
        
        Long userId1 = jwtService.subjectToUserId(token1);
        Long userId2 = jwtService.subjectToUserId(token2);
        
        assertThat(userId1).isEqualTo(1L);
        assertThat(userId2).isEqualTo(2L);
    }

    @Test
    @DisplayName("Deve extrair mesmo user ID de tokens criados no mesmo segundo")
    void shouldExtractSameUserIdFromTokensCreatedInSameSecond() {
        // When
        String token1 = jwtService.createAcessToken(testUser);
        String token2 = jwtService.createAcessToken(testUser);

        // Then
        // Ambos devem extrair o mesmo user ID independente se são iguais ou não
        Long userId1 = jwtService.subjectToUserId(token1);
        Long userId2 = jwtService.subjectToUserId(token2);
        
        assertThat(userId1).isEqualTo(userId2).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("Deve falhar com token malformado")
    void shouldFailWithMalformedToken() {
        // Given
        String malformedToken = "not.a.valid.jwt.token";

        // When & Then
        assertThatThrownBy(() -> jwtService.subjectToUserId(malformedToken))
            .isInstanceOf(JwtException.class);
    }
}