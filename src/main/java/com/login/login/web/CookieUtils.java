// Pacote web - utilitários para a camada de apresentação
package com.login.login.web;

// Importações Jakarta Servlet (novo nome do javax.servlet)
import jakarta.servlet.http.Cookie;             // Classe para cookies HTTP
import jakarta.servlet.http.HttpServletResponse;  // Interface para resposta HTTP

/**
 * UTILITÁRIO PARA COOKIES SEGUROS
 * 
 * Classe auxiliar para criar e configurar cookies HTTP com segurança máxima.
 * 
 * PROBLEMA QUE RESOLVE:
 * - API padrão de Cookie do Java não suporta SameSite
 * - Configuração manual de cookies seguros é verbosa
 * - Centraliza configuração de segurança de cookies
 * 
 * SEGURANÇA DE COOKIES IMPLEMENTADA:
 * - HttpOnly: não acessível via JavaScript (previne XSS)
 * - Secure: só enviado via HTTPS (previne interceptação)
 * - SameSite: controla quando cookie é enviado (previne CSRF)
 * - Path: limita escopo do cookie
 * - Domain: controla domínios que recebem o cookie
 * - Max-Age: controla expiração
 * 
 * USO TÍPICO:
 * - Armazenar tokens JWT de forma segura
 * - Cookies de sessão
 * - Preferências do usuário
 */
public class CookieUtils {
    
    /**
     * CONSTRUIR COOKIE SEGURO
     * 
     * Cria cookie com todas as configurações de segurança aplicadas.
     * 
     * CONFIGURAÇÕES AUTOMÁTICAS:
     * - HttpOnly: true (não acessível via JS)
     * - Path: "/" (válido para toda aplicação)
     * - Secure: conforme parâmetro (deve ser true em produção)
     * 
     * @param name String nome do cookie
     * @param value String valor do cookie (ex: JWT token)
     * @param maxAgeSeconds int tempo de vida em segundos
     * @param domain String domínio (null para usar domínio atual)
     * @param secure boolean se deve usar HTTPS apenas
     * @param sameSite String valor SameSite (será usado em método separado)
     * @return Cookie objeto configurado
     */
    public static Cookie build(String name, String value, int maxAgeSeconds, String domain, boolean secure, String sameSite) {
        // CRIAR COOKIE BÁSICO
        var c = new Cookie(name, value);
        //       ↑     ↑      ↑
        //    Objeto  Nome   Valor
        
        // CONFIGURAR SEGURANÇA HTTPONLY
        c.setHttpOnly(true);
        //            ↑
        // Cookie não acessível via document.cookie (JavaScript)
        // Previne ataques XSS que tentam roubar cookies
        
        // CONFIGURAR ESCOPO
        c.setPath("/");
        //        ↑
        // Cookie válido para toda a aplicação (não só para rota específica)
        
        // CONFIGURAR EXPIRAÇÃO
        c.setMaxAge(maxAgeSeconds);
        //          ↑
        // Tempo em segundos até expirar
        // -1 = cookie de sessão (expira quando browser fecha)
        // 0 = deleta cookie imediatamente
        // >0 = expira após X segundos
        
        // CONFIGURAR DOMÍNIO (OPCIONAL)
        if (domain != null && !domain.isBlank()) c.setDomain(domain);
        //     ↑                                             ↑
        // Se informado                              Define domínio específico
        
        // CONFIGURAR HTTPS OBRIGATÓRIO
        c.setSecure(secure);
        //          ↑
        // true = só envia via HTTPS (produção)
        // false = envia via HTTP também (desenvolvimento)
        
        return c;  // Retorna cookie configurado
    }
    
    /**
     * ADICIONAR COOKIE COM SAMESITE
     * 
     * Adiciona cookie na resposta HTTP com suporte a SameSite.
     * 
     * PROBLEMA: API padrão Cookie não suporta SameSite
     * SOLUÇÃO: Montar header Set-Cookie manualmente
     * 
     * VALORES DE SAMESITE:
     * - "Strict": nunca envia em requisições cross-site
     * - "Lax": envia em navegação top-level (links)
     * - "None": sempre envia (requer Secure=true)
     * 
     * @param res HttpServletResponse para adicionar header
     * @param cookie Cookie objeto configurado
     * @param sameSite String valor SameSite desejado
     */
    public static void addWithSameSite(HttpServletResponse res, Cookie cookie, String sameSite) {
        // MONTAR HEADER SET-COOKIE MANUALMENTE
        String header = "%s=%s; Path=%s; Max-Age=%d; %s%s%s".formatted(
            cookie.getName(),          // Nome do cookie
            cookie.getValue(),         // Valor do cookie
            cookie.getPath(),          // Path (/)
            cookie.getMaxAge(),        // Tempo de vida
            cookie.getSecure() ? "Secure; " : "",  // Secure se configurado
            "HttpOnly; ",              // Sempre HttpOnly
            (sameSite != null ? "SameSite=" + sameSite : "")  // SameSite se informado
        );
        //   ↑
        // Exemplo: "ACCESS_TOKEN=eyJhbGc...; Path=/; Max-Age=3600; Secure; HttpOnly; SameSite=Lax"
        
        // ADICIONAR DOMÍNIO SE CONFIGURADO
        if (cookie.getDomain() != null) header += "; Domain=" + cookie.getDomain();
        
        // ADICIONAR HEADER NA RESPOSTA HTTP
        res.addHeader("Set-Cookie", header);
        //            ↑              ↑
        //    Nome do header    Valor completo
    }
    
    /*
     * MÉTODOS ADICIONAIS QUE PODERÍAMOS IMPLEMENTAR:
     * 
     * public static Cookie createJwtCookie(String token, int maxAgeSeconds) {
     *     return build("ACCESS_TOKEN", token, maxAgeSeconds, null, true, "Lax");
     * }
     * 
     * public static Cookie createRefreshCookie(String token, int maxAgeDays) {
     *     return build("REFRESH_TOKEN", token, maxAgeDays * 24 * 60 * 60, null, true, "Strict");
     * }
     * 
     * public static void deleteCookie(HttpServletResponse res, String name) {
     *     Cookie cookie = new Cookie(name, "");
     *     cookie.setMaxAge(0);  // Expira imediatamente
     *     cookie.setPath("/");
     *     res.addCookie(cookie);
     * }
     * 
     * public static Optional<String> getCookieValue(HttpServletRequest req, String name) {
     *     if (req.getCookies() != null) {
     *         return Arrays.stream(req.getCookies())
     *             .filter(c -> name.equals(c.getName()))
     *             .map(Cookie::getValue)
     *             .findFirst();
     *     }
     *     return Optional.empty();
     * }
     * 
     * EXEMPLO DE USO COMPLETO:
     * 
     * @PostMapping("/login")
     * public String login(@Valid LoginFormDto form, HttpServletResponse response) {
     *     // 1. Autenticar usuário
     *     User user = authService.authenticate(form.email(), form.password());
     *     
     *     // 2. Gerar token JWT
     *     String token = jwtService.createAccessToken(user);
     *     
     *     // 3. Criar cookie seguro
     *     Cookie cookie = CookieUtils.build(
     *         "ACCESS_TOKEN",    // nome
     *         token,             // valor (JWT)
     *         30 * 60,          // 30 minutos
     *         null,             // domínio atual
     *         true,             // HTTPS obrigatório
     *         "Lax"             // SameSite Lax
     *     );
     *     
     *     // 4. Adicionar na resposta
     *     CookieUtils.addWithSameSite(response, cookie, "Lax");
     *     
     *     return "redirect:/dashboard";
     * }
     * 
     * CONFIGURAÇÕES RECOMENDADAS POR AMBIENTE:
     * 
     * DESENVOLVIMENTO:
     * - Secure: false (permite HTTP)
     * - SameSite: "Lax"
     * - Domain: null (localhost)
     * 
     * PRODUÇÃO:
     * - Secure: true (HTTPS obrigatório)
     * - SameSite: "Lax" ou "Strict"
     * - Domain: ".meuapp.com" (subdomínios)
     * 
     * TESTES:
     * - Secure: false
     * - SameSite: "None" (permite cross-origin)
     * - Domain: null
     */
}