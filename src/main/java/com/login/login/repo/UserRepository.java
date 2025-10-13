// Pacote repo - contém os repositórios (camada de acesso a dados)
package com.login.login.repo;

// Importações Java
import java.util.Optional;  // Container que pode ou não conter um valor (evita NullPointerException)

// Importação Spring Data JPA
import org.springframework.data.jpa.repository.JpaRepository;  // Interface base para repositórios JPA

// Importação da nossa entidade
import com.login.login.domain.User;

/**
 * REPOSITÓRIO DE USUÁRIOS
 * 
 * Esta interface define como acessar dados de usuários no banco de dados.
 * Herda de JpaRepository, que já fornece operações CRUD básicas.
 * 
 * O Spring Data JPA cria automaticamente a implementação desta interface!
 * Você só define a interface, o Spring faz toda a implementação.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    //                                                    ↑     ↑
    //                                              Entidade   Tipo do ID
    
    /**
     * MÉTODO DE CONSULTA PERSONALIZADO
     * 
     * O Spring Data JPA cria automaticamente este método baseado no nome!
     * 
     * Padrão: findBy + NomeDoCampo
     * - findByEmail → SELECT * FROM users WHERE email = ?
     * - findByName → SELECT * FROM users WHERE name = ?
     * - findByEmailAndName → SELECT * FROM users WHERE email = ? AND name = ?
     * 
     * @param email Email do usuário a ser buscado
     * @return Optional<User> - pode conter um User ou estar vazio se não encontrar
     */
    Optional<User> findByEmail(String email); 
    
    /*
     * MÉTODOS HERDADOS DE JpaRepository (NÃO PRECISAMOS IMPLEMENTAR):
     * 
     * - save(User user)                    → INSERT ou UPDATE
     * - findById(Long id)                  → SELECT por ID
     * - findAll()                          → SELECT * FROM users
     * - deleteById(Long id)                → DELETE por ID
     * - count()                            → COUNT(*)
     * - existsById(Long id)                → Verifica se existe
     * - flush()                            → Força sincronização com banco
     * 
     * E muitos outros métodos úteis!
     */
}
