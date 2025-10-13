// Pacote domain - entidades do domínio da aplicação  
package com.login.login.domain;

// Importações Java
import java.time.Instant;  // Representa momento específico no tempo (UTC)

// Importações Jakarta Persistence (JPA)
import jakarta.persistence.*;  // Todas as anotações JPA para mapeamento objeto-relacional

// Importações Lombok - biblioteca para reduzir código boilerplate
import lombok.*;

/**
 * ENTIDADE TOKEN DE RESET DE SENHA
 * 
 * Representa tokens temporários para redefinir senhas esquecidas.
 * 
 * FLUXO DE RESET DE SENHA:
 * 1. Usuário clica "Esqueci minha senha"
 * 2. Sistema gera token único com expiração
 * 3. Token é enviado por email
 * 4. Usuário clica no link com token
 * 5. Sistema valida token (existe? não expirou? não foi usado?)
 * 6. Usuário define nova senha
 * 7. Token é marcado como usado (não pode ser reutilizado)
 * 
 * SEGURANÇA:
 * - Token tem vida curta (15-60 minutos)
 * - Uso único (flag 'used')
 * - Vinculado a usuário específico
 * - Token aleatório e único
 */
@Entity  // Marca como entidade JPA (vira tabela no banco)
@Getter     // Lombok: gera métodos get automaticamente
@Setter     // Lombok: gera métodos set automaticamente  
@AllArgsConstructor (access = AccessLevel.PRIVATE)   // Construtor completo privado
@NoArgsConstructor (access = AccessLevel.PROTECTED)  // Construtor vazio protegido (JPA precisa)
@Builder (toBuilder = true)  // Padrão Builder + permite modificar objetos existentes
public class PasswordResetToken {
    
    /**
     * CHAVE PRIMÁRIA
     * 
     * @Id - marca como chave primária da tabela
     * @GeneratedValue - valor gerado automaticamente pelo banco
     * IDENTITY - usa AUTO_INCREMENT (MySQL) ou SERIAL (PostgreSQL)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * TOKEN ÚNICO
     * 
     * String aleatória que identifica o token de reset.
     * 
     * nullable = false - campo obrigatório (NOT NULL)
     * unique = true - cada token deve ser único na tabela
     * 
     * Exemplo: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
     */
    @Column(nullable = false, unique = true)
    private String token;

    /**
     * USUÁRIO DONO DO TOKEN
     * 
     * @ManyToOne - muitos tokens podem pertencer a um usuário
     * optional = false - campo obrigatório (usuário sempre deve existir)  
     * fetch = LAZY - carrega usuário apenas quando acessado (otimização)
     * 
     * SQL gerado: FOREIGN KEY (user_id) REFERENCES users(id)
     */
    @ManyToOne(optional = false, fetch= FetchType.LAZY)
    private User user;

    /**
     * DATA DE EXPIRAÇÃO
     * 
     * Instant - momento específico no tempo (formato UTC)
     * Mais preciso que Date/LocalDateTime para timestamps
     * 
     * nullable = false - campo obrigatório
     * 
     * Exemplo de uso:
     * Instant expira = Instant.now().plusSeconds(3600); // expira em 1 hora
     */
    @Column(nullable = false)
    private Instant expiresAt; //timestamp em millis

    /**
     * FLAG DE USO
     * 
     * @Builder.Default - define valor padrão quando usar Builder
     * 
     * Controla se o token já foi utilizado:
     * - false = token ainda não foi usado (pode ser usado)
     * - true = token já foi usado (não pode mais ser usado)
     * 
     * IMPORTANTE: Evita ataques de replay (reutilizar mesmo token)
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean used = false; //se o token já foi usado
    
    /*
     * MÉTODOS AUTOMATICAMENTE GERADOS PELO LOMBOK:
     * 
     * GETTERS:
     * - getId()           → Long
     * - getToken()        → String  
     * - getUser()         → User
     * - getExpiresAt()    → Instant
     * - isUsed()          → boolean
     * 
     * SETTERS:
     * - setId(Long)           → void
     * - setToken(String)      → void
     * - setUser(User)         → void
     * - setExpiresAt(Instant) → void
     * - setUsed(boolean)      → void
     * 
     * BUILDER PATTERN:
     * PasswordResetToken token = PasswordResetToken.builder()
     *     .token(UUID.randomUUID().toString())
     *     .user(user)
     *     .expiresAt(Instant.now().plusSeconds(3600))
     *     .build();
     * 
     * CONSTRUTORES:
     * - PasswordResetToken()              → protegido (para JPA)
     * - PasswordResetToken(todos campos)  → privado (uso interno)
     * 
     * MÉTODOS ÚTEIS QUE PODERÍAMOS ADICIONAR:
     * 
     * public boolean isExpired() {
     *     return Instant.now().isAfter(this.expiresAt);
     * }
     * 
     * public boolean isValid() {
     *     return !used && !isExpired();
     * }
     */
}
