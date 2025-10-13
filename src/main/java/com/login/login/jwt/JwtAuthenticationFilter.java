// Pacote jwt - componentes relacionados a autenticação JWT
package com.login.login.jwt;

// Importações Java
import java.io.IOException;  // Para tratamento de exceções I/O
import java.util.Arrays;     // Para trabalhar com arrays de cookies

// Importações Spring Security
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;  // Token de autenticação
import org.springframework.security.core.context.SecurityContextHolder;                  // Contexto de segurança global
import org.springframework.web.filter.OncePerRequestFilter;                              // Filtro que executa uma vez por requisição
import org.springframework.lang.NonNull;                                                 // Anotação para parâmetros não nulos

// Importação do repositório
import com.login.login.repo.UserRepository;  // Acesso aos dados do usuário

// Importações Jakarta Servlet (novo nome do javax.servlet)
import jakarta.servlet.FilterChain;       // Cadeia de filtros
import jakarta.servlet.ServletException;  // Exceção de servlet
import jakarta.servlet.http.*;            // Classes HTTP (request, response, cookie)

/**
 * FILTRO DE AUTENTICAÇÃO JWT
 * 
 * Intercepta todas as requisições HTTP para verificar se o usuário está autenticado.
 * 
 * COMO FUNCIONA:
 * 1. Intercepta requisição HTTP
 * 2. Procura cookie "ACCESS_TOKEN"  
 * 3. Se encontra, decodifica JWT
 * 4. Se válido, carrega usuário e autentica
 * 5. Passa requisição adiante na cadeia de filtros
 * 
 * FILTROS NO SPRING SECURITY:
 * Client → SecurityFilterChain → Controller
 *           ↓
 *       [JwtAuthenticationFilter] ← Este filtro
 *       [UsernamePasswordAuthenticationFilter]
 *       [FilterSecurityInterceptor]
 *       [...]
 * 
 * VANTAGENS SOBRE SESSION:
 * - Stateless (não armazena estado no servidor)
 * - Escala horizontalmente  
 * - Funciona em micro-serviços
 * - Cross-domain friendly
 * 
 * SEGURANÇA:
 * - Token armazenado em cookie HttpOnly (não acessível via JS)
 * - Validação de assinatura a cada requisição
 * - Expiração automática
 * - Não armazena senha no token
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    /**
     * DEPENDÊNCIAS DO FILTRO
     * 
     * final = imutáveis (thread-safe)
     * Injetadas via construtor
     */
    private final JwtService jwt;        // Para decodificar e validar tokens
    private final UserRepository users;  // Para carregar dados do usuário

    /**
     * CONSTRUTOR COM INJEÇÃO DE DEPENDÊNCIA
     * 
     * Spring Security configura este filtro no SecurityConfig
     * e injeta as dependências necessárias.
     * 
     * @param jwt Serviço para processar tokens JWT
     * @param users Repositório para buscar usuários no banco
     */
    public JwtAuthenticationFilter(JwtService jwt, UserRepository users) {
        this.jwt = jwt;
        this.users = users;
    }

    /**
     * MÉTODO PRINCIPAL DO FILTRO
     * 
     * Executado uma vez por requisição HTTP (OncePerRequestFilter).
     * 
     * FLUXO DE EXECUÇÃO:
     * 1. Tenta extrair token JWT do cookie
     * 2. Se encontra token válido, autentica usuário
     * 3. Se não encontra ou inválido, continua sem autenticar
     * 4. Passa requisição para próximo filtro na cadeia
     * 
     * @param req HttpServletRequest com dados da requisição
     * @param res HttpServletResponse para a resposta
     * @param chain FilterChain para continuar processamento
     * @throws ServletException se erro no servlet
     * @throws IOException se erro I/O
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req, @NonNull HttpServletResponse res, @NonNull FilterChain chain)
        throws ServletException, IOException{
        
        try{
            // BUSCAR TODOS OS COOKIES DA REQUISIÇÃO
            var cookies = req.getCookies();
            //    ↑
            // Retorna Cookie[] ou null se não há cookies
            
            if(cookies != null){
                // PROCURAR COOKIE COM TOKEN DE ACESSO
                var c = Arrays.stream(cookies)
                    .filter(cookie -> "ACCESS_TOKEN".equals(cookie.getName()))
                    .findFirst();
                //  ↑
                // Optional<Cookie>
                
                if (c.isPresent()){
                    // DECODIFICAR JWT E EXTRAIR USER ID
                    var userID = jwt.subjectToUserId(c.get().getValue());
                    //              ↑                  ↑
                    //        JwtService        Valor do cookie
                    
                    // CARREGAR USUÁRIO DO BANCO DE DADOS
                    var user = users.findById(userID).orElse(null);
                    //          ↑                     ↑
                    //    Repository           Se não encontrar = null
                    
                    if (user != null){
                        // CRIAR TOKEN DE AUTENTICAÇÃO DO SPRING SECURITY
                        var auth = new UsernamePasswordAuthenticationToken(
                            user,                    // Principal (usuário autenticado)
                            null,                    // Credentials (não precisa da senha)
                            user.getAuthorities()    // Authorities (roles/permissões)
                        );
                        
                        // DEFINIR USUÁRIO COMO AUTENTICADO NO CONTEXTO GLOBAL
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        //                    ↑              ↑
                        //              Contexto global   Define autenticação
                        
                        /*
                         * A partir deste ponto:
                         * - SecurityContextHolder.getContext().getAuthentication() retorna nosso token
                         * - @AuthenticationPrincipal User user funcionará nos controllers
                         * - Verificações de autorização funcionarão normalmente
                         */
                    }
                }
            }
        
        }catch (Exception ignored){
            // IGNORAR QUALQUER ERRO
            // Se JWT inválido, expirado, usuário não existe, etc.
            // Simplesmente não autentica (usuário fica anônimo)
            // 
            // IMPORTANTE: Não fazer log detalhado aqui pois:
            // - Pode gerar muito ruído (tokens expirados são normais)
            // - Informações sensíveis podem vazar nos logs
        }
        
        // CONTINUAR CADEIA DE FILTROS
        chain.doFilter(req, res);
        //    ↑
        // SEMPRE executar, mesmo se autenticação falhou
        // Próximo filtro ou controller decidirá se autoriza ou não
    }

    /**
     * DEFINIR QUAIS ROTAS NÃO DEVEM SER FILTRADAS
     * 
     * Otimização: não executar este filtro em rotas que:
     * - Não precisam de autenticação (login, registro, assets)
     * - São recursos estáticos (CSS, JS, imagens)
     * 
     * IMPORTANTE: Este método é só otimização!
     * A autorização real é definida no SecurityConfig.
     * 
     * @param req HttpServletRequest
     * @return boolean true = pular este filtro, false = executar filtro
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest req){
        var p = req.getServletPath();  // Caminho da requisição
        
        // NÃO FILTRAR ESTAS ROTAS:
        return p.startsWith("/auth")     ||  // Rotas de autenticação (/auth/login, /auth/register)
               p.startsWith("/css")      ||  // Arquivos CSS
               p.startsWith("/js")       ||  // Arquivos JavaScript  
               p.startsWith("/images")   ||  // Imagens
               p.startsWith("/");            // Página inicial (pode ser pública)
        
        /*
         * ROTAS QUE SERÃO FILTRADAS:
         * - /dashboard
         * - /profile  
         * - /admin
         * - /api/users
         * - Qualquer outra rota protegida
         * 
         * ALTERNATIVAS MAIS SOFISTICADAS:
         * 
         * 1. Lista de rotas públicas:
         * Set<String> publicPaths = Set.of("/", "/auth/login", "/auth/register", "/auth/reset");
         * return publicPaths.contains(p);
         * 
         * 2. Regex patterns:
         * return p.matches("/(auth|css|js|images)/.*") || "/".equals(p);
         * 
         * 3. Integração com SecurityConfig:
         * return securityConfig.isPublicPath(p);
         */
    }
    
    /*
     * MELHORIAS POSSÍVEIS:
     * 
     * 1. SUPORTE A HEADER AUTHORIZATION:
     * 
     * String extractTokenFromRequest(HttpServletRequest req) {
     *     // 1. Tentar cookie primeiro
     *     Cookie[] cookies = req.getCookies();
     *     if (cookies != null) {
     *         return Arrays.stream(cookies)
     *             .filter(c -> "ACCESS_TOKEN".equals(c.getName()))
     *             .findFirst()
     *             .map(Cookie::getValue)
     *             .orElse(null);
     *     }
     *     
     *     // 2. Tentar header Authorization
     *     String header = req.getHeader("Authorization");
     *     if (header != null && header.startsWith("Bearer ")) {
     *         return header.substring(7);
     *     }
     *     
     *     return null;
     * }
     * 
     * 2. CACHE DE USUÁRIOS:
     * 
     * @Autowired
     * private CacheManager cacheManager;
     * 
     * User findUserWithCache(Long userId) {
     *     return cacheManager.getCache("users").get(userId, () -> 
     *         users.findById(userId).orElse(null)
     *     );
     * }
     * 
     * 3. MÉTRICAS E MONITORAMENTO:
     * 
     * @Autowired  
     * private MeterRegistry meterRegistry;
     * 
     * private void recordAuthenticationAttempt(boolean success) {
     *     meterRegistry.counter("auth.jwt.attempts", "success", String.valueOf(success)).increment();
     * }
     * 
     * 4. LOGGING DE SEGURANÇA:
     * 
     * private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY");
     * 
     * private void logSuspiciousActivity(String token, Exception e) {
     *     securityLogger.warn("JWT validation failed: {}", e.getMessage());
     * }
     */
}
