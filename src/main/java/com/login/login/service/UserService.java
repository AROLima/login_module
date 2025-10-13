// Pacote service - contém a lógica de negócio da aplicação
package com.login.login.service;

// Importações Spring Security
import org.springframework.security.crypto.password.PasswordEncoder;  // Interface para hash de senhas
import org.springframework.stereotype.Service;                        // Marca como componente de serviço

// Importações das nossas classes
import com.login.login.domain.User;           // Entidade usuário
import com.login.login.repo.UserRepository;   // Repositório para acesso a dados

// Importação de transação
import jakarta.transaction.Transactional;  // Controle de transação de banco de dados

/**
 * SERVIÇO DE USUÁRIOS
 * 
 * Camada de serviço contém a LÓGICA DE NEGÓCIO da aplicação.
 * 
 * RESPONSABILIDADES:
 * - Validações de regras de negócio
 * - Coordenação entre repositórios
 * - Transformações de dados
 * - Controle de transações
 * 
 * ARQUITETURA EM CAMADAS:
 * Controller → Service → Repository → Database
 *     ↓          ↓          ↓           ↓
 * Web/API → Lógica → Acesso → Banco
 * 
 * DIFERENÇA ENTRE CAMADAS:
 * - Controller: recebe requisições, valida entrada, chama service
 * - Service: aplica regras de negócio, coordena operações
 * - Repository: acesso direto aos dados, queries
 * - Domain: entidades e regras do domínio
 */
@Service  // Marca como componente Spring de serviço (será gerenciado pelo container)
public class UserService {
    
    /**
     * DEPENDÊNCIAS INJETADAS
     * 
     * final = imutáveis após construção (boa prática)
     * 
     * Spring injeta automaticamente via construtor (Dependency Injection)
     */
    private final UserRepository userRepository;    // Para operações de banco
    private final PasswordEncoder passwordEncoder;  // Para hash de senhas

    /**
     * CONSTRUTOR COM INJEÇÃO DE DEPENDÊNCIA
     * 
     * Spring automaticamente injeta as dependências:
     * - UserRepository: implementação criada pelo Spring Data JPA
     * - PasswordEncoder: BCryptPasswordEncoder configurado no SecurityConfig
     * 
     * VANTAGENS DA INJEÇÃO POR CONSTRUTOR:
     * - Dependências obrigatórias (final)
     * - Fácil para testes (mock das dependências)
     * - Imutável após construção
     * - Falha rápido se dependência não existe
     */
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * CRIAR NOVO USUÁRIO
     * 
     * @Transactional - garante que toda operação seja atômica
     *                 Se der erro, faz rollback automático
     *                 Se sucesso, faz commit automático
     * 
     * REGRAS DE NEGÓCIO APLICADAS:
     * 1. Email deve ser único no sistema
     * 2. Senha deve ser hasheada antes de salvar
     * 3. Usuário criado deve ter status ativo
     * 
     * @param email Email do usuário (usado como username)
     * @param password Senha em texto plano (será hasheada)
     * @param name Nome completo do usuário
     * @return User Usuário criado e salvo no banco
     * @throws IllegalArgumentException se email já existe
     */
    @Transactional
    public User createUser(String email, String password, String name) {
        // VALIDAÇÃO DE REGRA DE NEGÓCIO: Email único
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email já cadastrado");
        }
        //                                    ↑
        //               Optional.isPresent() retorna true se encontrou usuário

        // CRIAR NOVO USUÁRIO
        // User.ofnew() é factory method que cria usuário com valores padrão
        // passwordEncoder.encode() aplica BCrypt com salt automático
        User user = User.ofnew(email, passwordEncoder.encode(password), name);
        //                            ↑
        //           BCrypt transforma "senha123" → "$2a$10$N9qo8uLOickgx2ZMRZoMye..."

        // SALVAR NO BANCO DE DADOS
        return userRepository.save(user);
        //     ↑
        //   JPA faz INSERT e retorna entidade com ID gerado
    }

    /**
     * VERIFICAR SE EMAIL JÁ EXISTE
     * 
     * Método auxiliar para validações.
     * Usado antes de tentar criar usuário.
     * 
     * @param email Email a ser verificado
     * @return boolean true se email já está cadastrado, false se disponível
     */
    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
        //     ↑                                  ↑
        //  Repository         Optional.isPresent() → boolean
    }
    
    /*
     * MÉTODOS ADICIONAIS QUE PODERÍAMOS IMPLEMENTAR:
     * 
     * public User findByEmail(String email) {
     *     return userRepository.findByEmail(email)
     *         .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));
     * }
     * 
     * public User updateUser(Long userId, String name) {
     *     User user = findById(userId);
     *     user.setName(name);
     *     return userRepository.save(user);
     * }
     * 
     * @Transactional
     * public void changePassword(Long userId, String oldPassword, String newPassword) {
     *     User user = findById(userId);
     *     
     *     if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
     *         throw new InvalidPasswordException("Senha atual incorreta");
     *     }
     *     
     *     user.setPassword(passwordEncoder.encode(newPassword));
     *     userRepository.save(user);
     * }
     * 
     * public boolean isValidPassword(String rawPassword, String encodedPassword) {
     *     return passwordEncoder.matches(rawPassword, encodedPassword);
     * }
     * 
     * @Transactional
     * public void deleteUser(Long userId) {
     *     if (!userRepository.existsById(userId)) {
     *         throw new UserNotFoundException("Usuário não encontrado");
     *     }
     *     userRepository.deleteById(userId);
     * }
     * 
     * public List<User> findAllUsers() {
     *     return userRepository.findAll();
     * }
     * 
     * public Page<User> findUsersWithPagination(int page, int size) {
     *     Pageable pageable = PageRequest.of(page, size);
     *     return userRepository.findAll(pageable);
     * }
     */
}