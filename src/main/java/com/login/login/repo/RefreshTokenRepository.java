// Pacote repo - repositórios para acesso a dados
package com.login.login.repo;

// Importações Java
import java.util.Optional;  // Container seguro para valores que podem ser nulos

// Importação Spring Data JPA
import org.springframework.data.jpa.repository.JpaRepository;  // Interface base para operações CRUD

// Importação da nossa entidade
import com.login.login.domain.RefreshToken;

/**
 * REPOSITÓRIO DE TOKENS DE REFRESH
 * 
 * Gerencia os tokens de refresh no banco de dados.
 * Tokens de refresh são usados para renovar tokens JWT sem fazer login novamente.
 * 
 * Este repositório permite:
 * - Salvar novos tokens de refresh
 * - Buscar tokens pelo hash
 * - Excluir tokens expirados ou invalidados
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    //                                                        ↑           ↑
    //                                                   Entidade    Tipo do ID
    
    /**
     * BUSCA TOKEN PELO HASH
     * 
     * Método gerado automaticamente pelo Spring Data JPA.
     * 
     * Padrão: findBy + NomeDoCampo
     * → SQL: SELECT * FROM refresh_tokens WHERE token_hash = ?
     * 
     * @param tokenHash Hash do token a ser buscado
     * @return Optional<RefreshToken> - token encontrado ou vazio
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    
    /**
     * EXCLUI TOKEN PELO HASH
     * 
     * Método gerado automaticamente pelo Spring Data JPA.
     * 
     * Padrão: deleteBy + NomeDoCampo
     * → SQL: DELETE FROM refresh_tokens WHERE token_hash = ?
     * 
     * ATENÇÃO: Este método precisa de @Transactional no serviço que o chama!
     * 
     * @param tokenHash Hash do token a ser excluído
     */
    void deleteByTokenHash(String tokenHash);
    
    /*
     * MÉTODOS HERDADOS AUTOMATICAMENTE:
     * 
     * - save(RefreshToken token)           → INSERT ou UPDATE
     * - findById(Long id)                  → SELECT por ID  
     * - findAll()                          → SELECT * FROM refresh_tokens
     * - deleteById(Long id)                → DELETE por ID
     * - count()                            → COUNT(*)
     * - existsById(Long id)                → Verifica se existe
     * 
     * E muitos outros métodos úteis do JpaRepository!
     */
}
