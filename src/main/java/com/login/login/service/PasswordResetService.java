// Pacote service - serviços com lógica de negócio
package com.login.login.service;

// Importações Java
import java.time.Instant;  // Para trabalhar com timestamps UTC
import java.util.UUID;     // Para gerar tokens únicos

// Importações Spring
import org.springframework.beans.factory.annotation.Value;  // Injeta valores de configuração
import org.springframework.security.crypto.password.PasswordEncoder;  // Interface para hash de senhas
import org.springframework.stereotype.Service;  // Marca como componente de serviço

// Importações das nossas classes
import com.login.login.domain.PasswordResetToken;  // Entidade do token de reset
import com.login.login.mail.MailService;           // Serviço de envio de emails
import com.login.login.repo.PasswordResetTokenRepository;  // Repositório de tokens
import com.login.login.repo.UserRepository;        // Repositório de usuários

// Importação de transação
import jakarta.transaction.Transactional;  // Controle de transações de banco

/**
 * SERVIÇO DE RESET DE SENHA
 * 
 * Gerencia todo o processo de redefinição de senhas esquecidas.
 * 
 * FLUXO COMPLETO:
 * 1. Usuário solicita reset informando email
 * 2. Sistema gera token único com expiração
 * 3. Token é enviado por email
 * 4. Usuário clica no link e acessa formulário
 * 5. Usuário informa nova senha
 * 6. Sistema valida token e atualiza senha
 * 7. Token é marcado como usado
 * 
 * SEGURANÇA IMPLEMENTADA:
 * - Token único (UUID) impossível de adivinhar
 * - Expiração configurável (tempo limitado)
 * - Uso único (não pode ser reutilizado)
 * - Senha hasheada com BCrypt
 * - Não revela se email existe (proteção contra enumeração)
 */
@Service  // Componente Spring gerenciado pelo container de IoC
public class PasswordResetService {
    
    /**
     * DEPENDÊNCIAS INJETADAS
     * 
     * final = imutáveis após construção (thread-safe)
     * Todas injetadas via construtor (Dependency Injection)
     */
    private final UserRepository users;                    // Acesso aos usuários
    private final PasswordResetTokenRepository tokens;    // Acesso aos tokens de reset
    private final PasswordEncoder encoder;                // Hash de senhas (BCrypt)
    private final MailService mail;                       // Envio de emails

    /**
     * CONFIGURAÇÃO EXTERNA
     * 
     * @Value injeta valor do application.yml/properties
     * 
     * Exemplo no application.yml:
     * app:
     *   security:
     *     reset-token-expiration-minutes: 30
     * 
     * VANTAGENS:
     * - Configuração externa (não hard-coded)
     * - Diferentes valores por ambiente (dev/prod)
     * - Fácil alteração sem recompilar código
     */
    @Value("${app.security.reset-token-expiration-minutes}")
    long ttl;  // Time To Live = tempo de vida do token em minutos

    /**
     * CONSTRUTOR COM INJEÇÃO DE DEPENDÊNCIA
     * 
     * Spring automaticamente injeta todas as dependências.
     * Parâmetros com nomes curtos para legibilidade.
     * 
     * @param u UserRepository
     * @param t PasswordResetTokenRepository  
     * @param e PasswordEncoder
     * @param m MailService
     */
    public PasswordResetService (UserRepository u, PasswordResetTokenRepository t, PasswordEncoder e, MailService m) {
        this.users = u;
        this.tokens = t;
        this.encoder = e;
        this.mail = m;
    }

    /**
     * SOLICITAR RESET DE SENHA
     * 
     * @Transactional - operação atômica (rollback em caso de erro)
     * 
     * SEGURANÇA - PROTEÇÃO CONTRA ENUMERAÇÃO:
     * - Não revela se email existe ou não
     * - Sempre retorna sucesso para o usuário
     * - Se email não existe, simplesmente não envia email
     * - Impede ataques para descobrir emails cadastrados
     * 
     * @param email Email do usuário que esqueceu a senha
     */
    @Transactional
    public void request(String email) {
        // BUSCAR USUÁRIO POR EMAIL
        var user = users.findByEmail(email).orElse(null);
        //         ↑                        ↑
        //    Repository           Optional.orElse(null)
        
        // PROTEÇÃO CONTRA ENUMERAÇÃO DE EMAILS
        if (user == null) {
            return;  // Não revela que email não existe
        }
        //  ↑
        // Email não cadastrado → sai silenciosamente
        // Usuário não sabe se email existe ou não

        // CRIAR TOKEN DE RESET
        var prt = PasswordResetToken.builder()
            .token(UUID.randomUUID().toString())  // Token único: "a1b2c3d4-..."
            .user(user)                           // Vincula ao usuário
            .expiresAt(Instant.now().plusSeconds(ttl * 60))  // Expira em X minutos
            .build();
        //    ↑
        // Builder pattern do Lombok

        // SALVAR TOKEN NO BANCO
        tokens.save(prt);

        // ENVIAR EMAIL COM LINK DE RESET
        mail.sendResetEmail(user.getEmail(), prt.getToken());
        //                                   ↑
        //                          Link: /reset-password?token=abc123
    }
    
    /**
     * PROCESSAR RESET DE SENHA
     * 
     * @Transactional - garante atomicidade:
     * - Se der erro, faz rollback (senha não muda, token não é marcado como usado)
     * - Se sucesso, faz commit (senha atualizada, token marcado como usado)
     * 
     * VALIDAÇÕES APLICADAS:
     * 1. Token existe e não foi usado
     * 2. Token não expirou
     * 3. Nova senha é hasheada
     * 4. Token marcado como usado
     * 
     * @param token Token recebido via URL
     * @param newPassword Nova senha em texto plano
     * @throws IllegalArgumentException se token inválido ou expirado
     */
    @Transactional
    public void reset(String token, String newPassword){
        // BUSCAR TOKEN VÁLIDO (NÃO USADO)
        var prt = tokens.findByTokenAndUsedFalse(token)
            .orElseThrow(() -> new IllegalArgumentException("Invalid token"));
        //    ↑                                            ↑
        // Optional.orElseThrow()                     Exceção se não encontrar

        // VERIFICAR SE TOKEN NÃO EXPIROU  
        if (prt.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token expired");   
        }
        //     ↑                    ↑
        // Data expiração    Data atual

        // ATUALIZAR SENHA DO USUÁRIO
        var user = prt.getUser();  // Usuário dono do token
        user.setPassword(encoder.encode(newPassword));  // Nova senha hasheada
        //               ↑
        //          BCrypt: "senha123" → "$2a$10$N9qo8uLO..."

        // MARCAR TOKEN COMO USADO
        prt.setUsed(true);  // Evita reutilização do mesmo token
        
        // SALVAR ALTERAÇÕES NO BANCO
        users.save(user);    // Atualiza senha do usuário
        tokens.save(prt);    // Marca token como usado
    }
    
    /*
     * MÉTODOS ADICIONAIS QUE PODERÍAMOS IMPLEMENTAR:
     * 
     * public boolean isValidToken(String token) {
     *     return tokens.findByTokenAndUsedFalse(token)
     *         .map(prt -> prt.getExpiresAt().isAfter(Instant.now()))
     *         .orElse(false);
     * }
     * 
     * @Transactional
     * public void invalidateAllTokensForUser(String email) {
     *     var user = users.findByEmail(email).orElse(null);
     *     if (user != null) {
     *         // Marcar todos tokens do usuário como usados
     *         var userTokens = tokens.findByUserAndUsedFalse(user);
     *         userTokens.forEach(token -> token.setUsed(true));
     *         tokens.saveAll(userTokens);
     *     }
     * }
     * 
     * @Scheduled(cron = "0 0 1 * * ?") // Todo dia às 1h da manhã
     * @Transactional
     * public void cleanupExpiredTokens() {
     *     Instant oneDayAgo = Instant.now().minusSeconds(86400);
     *     tokens.deleteByExpiresAtBefore(oneDayAgo);
     * }
     * 
     * public void requestWithRateLimit(String email) {
     *     // Verificar se não há muitas tentativas recentes do mesmo email
     *     long recentRequests = tokens.countByUserEmailAndCreatedAtAfter(
     *         email, Instant.now().minusSeconds(300)); // 5 minutos
     *     
     *     if (recentRequests >= 3) {
     *         throw new TooManyRequestsException("Muitas tentativas. Tente novamente em 5 minutos.");
     *     }
     *     
     *     request(email);
     * }
     * 
     * CONFIGURAÇÕES RECOMENDADAS:
     * 
     * application.yml:
     * app:
     *   security:
     *     reset-token-expiration-minutes: 30  # 30 minutos
     *   mail:
     *     reset-link-base-url: "https://meuapp.com/reset-password"
     */
}
