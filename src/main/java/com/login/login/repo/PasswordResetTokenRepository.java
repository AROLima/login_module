// Pacote repo - repositórios para acesso a dados
package com.login.login.repo;

// Importações Java
import java.util.Optional;  // Container seguro que pode ou não conter um valor

// Importação Spring Data JPA
import org.springframework.data.jpa.repository.JpaRepository;  // Interface base para CRUD

// Importação da nossa entidade
import com.login.login.domain.PasswordResetToken;

/**
 * REPOSITÓRIO DE TOKENS DE RESET DE SENHA
 * 
 * Gerencia tokens usados para redefinir senhas esquecidas.
 * 
 * Fluxo típico:
 * 1. Usuário esquece senha → sistema gera token
 * 2. Token é enviado por email
 * 3. Usuário clica no link com token
 * 4. Sistema valida token e permite nova senha
 * 5. Token é marcado como usado (não pode ser reutilizado)
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    //                                                              ↑                ↑
    //                                                         Entidade        Tipo do ID
    
    /**
     * BUSCA TOKEN SIMPLES
     * 
     * Encontra um token pelo valor, independente se já foi usado ou não.
     * 
     * Método gerado automaticamente pelo Spring Data JPA:
     * → SQL: SELECT * FROM password_reset_tokens WHERE token = ?
     * 
     * @param token Valor do token a ser buscado
     * @return Optional<PasswordResetToken> - token encontrado ou vazio
     */
    Optional<PasswordResetToken> findByToken(String token);
    
    /**
     * BUSCA TOKEN VÁLIDO (NÃO USADO)
     * 
     * Encontra um token que ainda não foi utilizado.
     * Evita que o mesmo token seja usado múltiplas vezes.
     * 
     * Método com múltiplas condições:
     * → SQL: SELECT * FROM password_reset_tokens WHERE token = ? AND used = false
     * 
     * Padrão Spring Data: findBy + Campo1 + And + Campo2 + Valor
     * - TokenAndUsedFalse → token = ? AND used = false
     * - TokenAndUsedTrue → token = ? AND used = true  
     * - UserIdAndUsedFalse → user_id = ? AND used = false
     * 
     * @param token Valor do token a ser buscado
     * @return Optional<PasswordResetToken> - token válido encontrado ou vazio
     */
    Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);
    
    /*
     * MÉTODOS HERDADOS AUTOMATICAMENTE DE JpaRepository:
     * 
     * - save(PasswordResetToken token)     → INSERT ou UPDATE
     * - findById(Long id)                  → SELECT por ID
     * - findAll()                          → SELECT * FROM password_reset_tokens
     * - deleteById(Long id)                → DELETE por ID
     * - count()                            → COUNT(*)
     * - existsById(Long id)                → Verifica se existe
     * 
     * MÉTODOS ADICIONAIS QUE PODERÍAMOS CRIAR:
     * 
     * - findByUserEmail(String email)             → Tokens por email do usuário
     * - findByCreatedAtAfter(LocalDateTime date)  → Tokens criados após data
     * - deleteByUsedTrueAndCreatedAtBefore(date)  → Limpar tokens antigos e usados
     * - countByUserEmailAndUsedFalse(String email) → Contar tokens ativos por usuário
     */
}
