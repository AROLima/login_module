// Pacote de configurações - centraliza todas as configurações da aplicação
package com.login.login.config;

// Importações do Spring Framework para configuração de beans
import org.springframework.context.annotation.Bean;        // Anotação para definir beans
import org.springframework.context.annotation.Configuration; // Marca esta classe como classe de configuração

// Importações do Spring Security para autenticação e autorização
import org.springframework.security.authentication.AuthenticationManager;                    // Gerencia autenticação
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration; // Configuração de autenticação
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;              // Habilita segurança em métodos
import org.springframework.security.config.annotation.web.builders.HttpSecurity;                              // Configuração de segurança HTTP
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;                    // Habilita segurança web
import org.springframework.security.config.http.SessionCreationPolicy;                                        // Política de criação de sessões
import org.springframework.security.core.userdetails.UserDetailsService;                                      // Serviço para buscar detalhes do usuário
import org.springframework.security.core.userdetails.UsernameNotFoundException;                               // Exceção quando usuário não é encontrado
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;                                      // Codificador de senha BCrypt (mais seguro)
import org.springframework.security.crypto.password.PasswordEncoder;                                          // Interface para codificação de senhas
import org.springframework.security.web.SecurityFilterChain;                                                  // Cadeia de filtros de segurança

// Importação do nosso repositório de usuários
import com.login.login.repo.UserRepository;

/**
 * CLASSE DE CONFIGURAÇÃO DE SEGURANÇA
 * Esta classe é o coração da segurança da aplicação.
 * Ela define como a autenticação e autorização funcionam.
 */
@Configuration               // Informa ao Spring que esta classe contém configurações (beans)
@EnableWebSecurity          // Ativa o Spring Security para aplicações web
@EnableMethodSecurity(prePostEnabled = true)  // Permite usar @PreAuthorize, @PostAuthorize em métodos
public class SecurityConfig {

    /**
     * BEAN: CODIFICADOR DE SENHAS
     * BCrypt é um algoritmo de hash muito seguro para senhas.
     * Ele adiciona "salt" (dados aleatórios) e é computacionalmente caro,
     * dificultando ataques de força bruta.
     * 
     * @return BCryptPasswordEncoder configurado com força padrão (10 rounds)
     */
    @Bean  // Registra este método como um bean do Spring (singleton por padrão)
    public PasswordEncoder passwordEncoder() {
        // BCrypt é considerado o melhor algoritmo para hash de senhas atualmente
        // Ele gera um hash diferente a cada execução, mesmo para a mesma senha
        return new BCryptPasswordEncoder();
    }

    /**
     * BEAN: SERVIÇO DE DETALHES DO USUÁRIO
     * Este bean define COMO o Spring Security deve buscar um usuário no banco de dados
     * durante o processo de autenticação.
     * 
     * @param userRepository Repositório JPA injetado automaticamente pelo Spring
     * @return Lambda function que implementa UserDetailsService
     */
    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        // Retorna uma função lambda que implementa UserDetailsService
        // Esta função será chamada sempre que alguém tentar fazer login
        return username -> userRepository.findByEmail(username)  // Busca usuário pelo email (que usamos como username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username)); // Se não encontrar, lança exceção
    }

    /**
     * BEAN: GERENCIADOR DE AUTENTICAÇÃO
     * O AuthenticationManager é responsável por coordenar o processo de autenticação.
     * Ele usa o UserDetailsService e PasswordEncoder para validar credenciais.
     * 
     * @param config Configuração de autenticação fornecida pelo Spring Security
     * @return AuthenticationManager configurado
     * @throws Exception Se houver erro na configuração
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        // Obtém o AuthenticationManager padrão do Spring Security 6.x
        // Este manager irá usar nosso UserDetailsService e PasswordEncoder automaticamente
        return config.getAuthenticationManager();
    }

    /**
     * BEAN: CADEIA DE FILTROS DE SEGURANÇA
     * Este é o bean mais importante! Ele define TODAS as regras de segurança da aplicação:
     * - Quais URLs precisam de autenticação
     * - Como funciona o login e logout
     * - Configurações de sessão
     * - Proteções de segurança
     * 
     * @param http Objeto HttpSecurity para configurar a segurança web
     * @return SecurityFilterChain configurada
     * @throws Exception Se houver erro na configuração
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            // === CONFIGURAÇÃO CSRF ===
            .csrf(csrf -> csrf.disable())  // CSRF (Cross-Site Request Forgery) desabilitado para simplificar
                                           // Em produção, você deve habilitá-lo para maior segurança
            
            // === CONFIGURAÇÃO DE SESSÕES ===
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                                           // IF_REQUIRED: Cria sessão apenas se necessário (padrão web)
                                           // Alternativas: ALWAYS, NEVER, STATELESS (para APIs REST)
            
            // === CONFIGURAÇÃO DE AUTORIZAÇÃO ===
            .authorizeHttpRequests(auth -> auth
                // URLs PÚBLICAS (não precisam de login):
                .requestMatchers("/auth/**", "/css/**", "/js/**", "/images/**", "/").permitAll()
                                           // /auth/** = todas as páginas de autenticação (login, register, etc.)
                                           // /css/**, /js/**, /images/** = recursos estáticos (CSS, JavaScript, imagens)
                                           // / = página inicial
                
                .requestMatchers("/h2-console/**").permitAll()  // Console do banco H2 (apenas para desenvolvimento!)
                
                .anyRequest().authenticated()  // TODAS as outras URLs precisam de autenticação
            )
            
            // === CONFIGURAÇÃO DE LOGIN ===
            .formLogin(form -> form
                .loginPage("/auth/login")              // Página customizada de login (nossa página Thymeleaf)
                .loginProcessingUrl("/login")          // URL que processa o login (POST)
                .usernameParameter("username")         // Nome do campo username no formulário HTML
                .passwordParameter("password")         // Nome do campo password no formulário HTML
                .defaultSuccessUrl("/dashboard", true) // Para onde ir após login bem-sucedido (true = sempre)
                .failureUrl("/auth/login?error=true")  // Para onde ir se o login falhar
                .permitAll()                           // Permite acesso às URLs de login sem autenticação
            )
            
            // === CONFIGURAÇÃO DE LOGOUT ===
            .logout(logout -> logout
                .logoutUrl("/auth/logout")                    // URL para fazer logout (POST)
                .logoutSuccessUrl("/auth/login?logout")       // Para onde ir após logout bem-sucedido
                .permitAll()                                  // Permite logout sem autenticação adicional
            )
            
            // === CONFIGURAÇÃO DE HEADERS ===
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.disable()))  // Desabilita X-Frame-Options
                                                                        // Necessário para o H2 Console funcionar
                                                                        // Em produção, configure adequadamente!
            
            .build();  // Constrói e retorna a SecurityFilterChain configurada
    }
}