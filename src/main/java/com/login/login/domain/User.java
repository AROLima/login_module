// Pacote domain - contém as entidades de domínio (objetos de negócio)
package com.login.login.domain;

// Importações Java padrão
import java.util.Collection;  // Interface para coleções (List, Set, etc.)
import java.util.List;        // Implementação de lista

// Importações do Spring Security para autoridades e detalhes do usuário
import org.springframework.security.core.authority.SimpleGrantedAuthority;  // Implementação simples de autoridade/permissão
import org.springframework.security.core.GrantedAuthority;                  // Interface para autoridades
import org.springframework.security.core.userdetails.UserDetails;           // Interface que representa um usuário autenticado

// Importações JPA (Java Persistence API) para mapeamento objeto-relacional
import jakarta.persistence.*;  // Anotações JPA para mapeamento de entidades

// Importações Lombok para reduzir código boilerplate
import lombok.*;  // @Getter, @Setter, @Builder, etc.

/**
 * ENTIDADE USER
 * Esta classe representa um usuário no sistema.
 * Implementa UserDetails para integração com Spring Security.
 * Usa JPA para persistência no banco de dados.
 * Usa Lombok para reduzir código repetitivo.
 */
@Entity  // JPA: Marca esta classe como uma entidade do banco de dados
@Table(name = "users")  // JPA: Define o nome da tabela no banco (por padrão seria "user", mas é palavra reservada)

// === ANOTAÇÕES LOMBOK ===
@Getter   // Lombok: Gera automaticamente métodos getter para todos os campos
@Setter   // Lombok: Gera automaticamente métodos setter para todos os campos
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // Lombok: Construtor sem argumentos PROTEGIDO (JPA precisa dele)
@AllArgsConstructor(access = AccessLevel.PRIVATE)   // Lombok: Construtor com todos os argumentos PRIVADO (usado pelo Builder)
@Builder(toBuilder = true)  // Lombok: Gera padrão Builder para criar objetos de forma fluente
                           // toBuilder = true permite criar uma cópia modificável
public class User implements UserDetails {  // Implementa UserDetails para Spring Security

    // === CAMPOS DA ENTIDADE ===
    
    /**
     * ID: Chave primária da tabela
     * IDENTITY: O banco gera automaticamente (auto-increment)
     */
    @Id  // JPA: Marca como chave primária
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // JPA: Geração automática de ID
    private Long id;

    /**
     * EMAIL: Campo único e obrigatório
     * Usado como username no sistema de autenticação
     */
    @Column(nullable = false, unique = true)  // JPA: Campo obrigatório e único no banco
    private String email;

    /**
     * PASSWORD: Senha do usuário (sempre armazenada com hash BCrypt)
     */
    @Column(nullable = false)  // JPA: Campo obrigatório
    private String password;

    /**
     * NAME: Nome completo do usuário
     */
    @Column(nullable = false)  // JPA: Campo obrigatório
    private String name;
    
    /**
     * ENABLED: Indica se a conta do usuário está ativa
     * Por padrão, novos usuários são habilitados (true)
     */
    @Builder.Default  // Lombok: Define valor padrão no Builder
    private boolean enabled = true;

    /**
     * MÉTODO FACTORY ESTÁTICO
     * Forma conveniente de criar um novo usuário com valores padrão.
     * 
     * @param email Email do usuário
     * @param password Senha (já deve vir com hash BCrypt)
     * @param name Nome completo
     * @return Nova instância de User
     */
    public static User ofnew(String email, String password, String name) {
        return User.builder()      // Usa o Builder gerado pelo Lombok
            .email(email)          // Define o email
            .password(password)    // Define a senha
            .name(name)           // Define o nome
            .enabled(true)        // Usuário ativo por padrão
            .build();             // Constrói o objeto
    }

    // === IMPLEMENTAÇÃO DOS MÉTODOS DO USERDETAILS ===
    // Estes métodos são exigidos pelo Spring Security
    
    /**
     * AUTORIDADES/PERMISSÕES do usuário
     * No nosso sistema simples, todos os usuários têm apenas ROLE_USER
     * 
     * @return Collection de autoridades concedidas
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // MVP (Minimum Viable Product): Todos os usuários têm a mesma permissão
        // Em sistemas mais complexos, você teria diferentes roles: ADMIN, USER, MODERATOR, etc.
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
    
    /**
     * USERNAME usado para autenticação
     * No nosso sistema, usamos o email como username
     * 
     * @return String representando o username
     */
    @Override
    public String getUsername() {
        return email;  // Retorna email como username (decisão de design)
    }

    /**
     * CONTA NÃO EXPIRADA?
     * Permite implementar expiração de contas
     * 
     * @return true se a conta não está expirada
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;  // Nossas contas nunca expiram (por enquanto)
    }

    /**
     * CONTA NÃO BLOQUEADA?
     * Permite bloquear contas temporariamente (ex: após muitas tentativas de login)
     * 
     * @return true se a conta não está bloqueada
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;  // Nossas contas nunca são bloqueadas (por enquanto)
    }

    /**
     * CREDENCIAIS NÃO EXPIRADAS?
     * Permite forçar mudança de senha periodicamente
     * 
     * @return true se as credenciais não expiraram
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;  // Nossas senhas nunca expiram (por enquanto)
    }

    /**
     * CONTA HABILITADA?
     * Usa o campo 'enabled' para determinar se o usuário pode fazer login
     * 
     * @return true se a conta está habilitada
     */
    @Override
    public boolean isEnabled() {
        return enabled;  // Retorna o valor do campo 'enabled'
    }
}
