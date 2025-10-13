// Pacote domain - contém as entidades do domínio da aplicação
package com.login.login.domain;

// Importações Java
import java.time.Instant;  // Representa um ponto no tempo (timestamp UTC)

// Importações Jakarta Persistence (JPA) - novo nome do javax.persistence
import jakarta.persistence.*;           // Todas as anotações JPA
import jakarta.persistence.GenerationType; // Estratégias de geração de ID
import jakarta.persistence.Id;             // Marca campo como chave primária  
import jakarta.persistence.ManyToOne;      // Relacionamento muitos-para-um
import jakarta.persistence.Table;          // Configuração da tabela

// Importações Lombok - reduz código boilerplate
import lombok.*;

/**
 * ENTIDADE REFRESH TOKEN
 * 
 * Representa tokens de refresh no sistema de autenticação JWT.
 * 
 * CONCEITO JWT:
 * - Access Token: Tem vida curta (15-30 min), usado para autenticar requisições
 * - Refresh Token: Tem vida longa (7-30 dias), usado para renovar access tokens
 * 
 * SEGURANÇA:
 * - Armazena apenas o HASH do token (nunca o valor original)
 * - Pode ser revogado manualmente
 * - Tem data de expiração
 * - Vinculado a um usuário específico
 */
@Entity  // Marca como entidade JPA (será uma tabela no banco)
@Table(name = "refresh_token")  // Nome da tabela no banco de dados
@Getter     // Lombok: gera automaticamente métodos get para todos os campos
@Setter     // Lombok: gera automaticamente métodos set para todos os campos
@NoArgsConstructor (access = AccessLevel.PROTECTED)  // Construtor sem argumentos protegido para JPA
@AllArgsConstructor (access = AccessLevel.PRIVATE)   // Construtor com todos argumentos privado para uso interno
@Builder (toBuilder = true)  // Lombok: padrão Builder + permite criar cópias modificadas
public class RefreshToken {

    /**
     * CHAVE PRIMÁRIA
     * 
     * @Id - marca como chave primária
     * @GeneratedValue - valor gerado automaticamente
     * IDENTITY - usa AUTO_INCREMENT do banco (MySQL, PostgreSQL, etc.)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * RELACIONAMENTO COM USUÁRIO
     * 
     * @ManyToOne - muitos tokens podem pertencer a um usuário
     * optional = false - campo obrigatório (NOT NULL)
     * fetch = LAZY - carrega usuário apenas quando necessário (performance)
     * 
     * SQL: FOREIGN KEY (user_id) REFERENCES users(id)
     */
    @ManyToOne(optional = false, fetch= FetchType.LAZY)
    private User user;

    /**
     * HASH DO TOKEN
     * 
     * SEGURANÇA: Nunca armazene tokens em plain text!
     * - Token original: "eyJhbGciOiJIUzI1NiJ9..."
     * - Armazenado: hash SHA-256 do token
     * 
     * nullable = false - campo obrigatório
     * unique = true - cada hash deve ser único
     * length = 200 - tamanho máximo do campo
     */
    @Column(nullable = false, unique = true, length = 200)
    private String tokenHash; // guarda o hash do refresh não o plaintext

    /**
     * DATA DE EXPIRAÇÃO
     * 
     * Instant - representa momento específico no tempo (UTC)
     * Mais preciso que Date ou LocalDateTime para timestamps
     * 
     * nullable = false - campo obrigatório
     */
    @Column (nullable = false)
    private Instant expiresAt; //validade do token

    /**
     * STATUS DE REVOGAÇÃO
     * 
     * @Builder.Default - define valor padrão no padrão Builder
     * 
     * Token pode ser revogado por:
     * - Logout manual do usuário
     * - Logout de todos os dispositivos  
     * - Detecção de atividade suspeita
     * - Mudança de senha
     * 
     * false = token válido
     * true = token revogado (não pode mais ser usado)
     */
    @Builder.Default
    @Column (nullable = false)
    private boolean revoked = false; //se o token foi revogado
    
    /*
     * MÉTODOS AUTOMATICAMENTE GERADOS PELO LOMBOK:
     * 
     * GETTERS:
     * - getId()          → retorna id
     * - getUser()        → retorna user
     * - getTokenHash()   → retorna tokenHash  
     * - getExpiresAt()   → retorna expiresAt
     * - isRevoked()      → retorna revoked
     * 
     * SETTERS:
     * - setId(Long)           → define id
     * - setUser(User)         → define user
     * - setTokenHash(String)  → define tokenHash
     * - setExpiresAt(Instant) → define expiresAt
     * - setRevoked(boolean)   → define revoked
     * 
     * BUILDER:
     * RefreshToken token = RefreshToken.builder()
     *     .user(user)
     *     .tokenHash("hash...")  
     *     .expiresAt(Instant.now().plusDays(30))
     *     .build();
     * 
     * CONSTRUTORES:
     * - RefreshToken()                    → construtor protegido (JPA)
     * - RefreshToken(todos os campos)     → construtor privado (interno)
     */
}
