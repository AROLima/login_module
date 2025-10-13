// Pacote web - camada de apresentação (controllers MVC)
package com.login.login.web;

// Importações Spring Security
import org.springframework.security.core.Authentication;  // Interface para usuário autenticado

// Importações Spring MVC
import org.springframework.stereotype.Controller;  // Marca como controller MVC
import org.springframework.ui.Model;              // Para passar dados para view
import org.springframework.web.bind.annotation.GetMapping;  // Para mapeamento HTTP GET

/**
 * CONTROLLER DO DASHBOARD
 * 
 * Responsável pelas páginas internas da aplicação (área logada).
 * 
 * PÁGINAS CONTROLADAS:
 * - Dashboard principal (área do usuário logado)
 * - Página inicial / (redireciona conforme autenticação)
 * 
 * DIFERENÇA PARA AuthPageController:
 * - AuthPageController: páginas públicas (login, registro, reset)
 * - DashboardController: páginas protegidas (requer autenticação)
 * 
 * SEGURANÇA:
 * - Rotas protegidas pelo Spring Security
 * - Se usuário não autenticado → redirecionado para login
 * - Se autenticado → acesso permitido
 */
@Controller  // Componente Spring MVC (retorna views, não JSON)
public class DashboardController {

    /**
     * PÁGINA DO DASHBOARD (ÁREA LOGADA)
     * 
     * Página principal após login bem-sucedido.
     * Mostra informações do usuário autenticado.
     * 
     * ROTA: GET /dashboard
     * 
     * PROTEÇÃO: Esta rota é protegida pelo SecurityConfig
     * - Se não autenticado → redirecionado para /auth/login
     * - Se autenticado → acesso permitido
     * 
     * @param model Model para passar dados para a view
     * @param authentication Authentication objeto com dados do usuário logado
     *                      - Injetado automaticamente pelo Spring Security
     *                      - null se não autenticado (mas isso não acontece aqui por causa da proteção)
     *                      - objeto completo se autenticado
     * @return String nome da view a ser renderizada
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        // VERIFICAR SE USUÁRIO ESTÁ AUTENTICADO
        if (authentication != null) {
            // PASSAR DADOS DO USUÁRIO PARA A VIEW
            
            // Nome do usuário (email neste caso, pois é o username)
            model.addAttribute("user", authentication.getName());
            //                   ↑              ↑
            //             Chave no template  Email do usuário
            
            // Roles/Permissões do usuário
            model.addAttribute("authorities", authentication.getAuthorities());
            //                   ↑                        ↑
            //             Chave no template      Collection<GrantedAuthority>
            //
            // Exemplo de authorities: [ROLE_USER, ROLE_ADMIN]
        }
        
        // RETORNAR NOME DA VIEW
        return "dashboard";
        //      ↑
        // ViewResolver procurará: /templates/dashboard.html
    }

    /**
     * PÁGINA INICIAL / ROOT
     * 
     * Redireciona usuário conforme seu status de autenticação:
     * - Se logado → vai para dashboard
     * - Se não logado → vai para login
     * 
     * ROTA: GET /
     * 
     * ESTRATÉGIA SMART REDIRECT:
     * - Evita mostrar página inicial desnecessária
     * - Leva usuário direto para onde deve estar
     * - Melhora UX (experiência do usuário)
     * 
     * @param authentication Authentication status do usuário
     * @return String redirect para rota apropriada
     */
    @GetMapping("/")
    public String home(Authentication authentication) {
        // VERIFICAR SE USUÁRIO ESTÁ AUTENTICADO E VÁLIDO
        if (authentication != null && authentication.isAuthenticated()) {
            //     ↑                         ↑
            // Objeto existe          Token é válido (não anônimo)
            
            return "redirect:/dashboard";  // Usuário logado → dashboard
        }
        
        return "redirect:/auth/login";  // Usuário anônimo → login
        //      ↑
        // HTTP 302 redirect (browser faz nova requisição)
    }
    
    /*
     * MÉTODOS ADICIONAIS QUE PODERÍAMOS IMPLEMENTAR:
     * 
     * @GetMapping("/profile")
     * public String profile(@AuthenticationPrincipal User user, Model model) {
     *     // @AuthenticationPrincipal injeta o objeto User diretamente
     *     model.addAttribute("user", user);
     *     return "profile";
     * }
     * 
     * @GetMapping("/settings")
     * public String settings(Model model, Authentication auth) {
     *     // Página de configurações do usuário
     *     User user = (User) auth.getPrincipal();
     *     model.addAttribute("user", user);
     *     return "settings";
     * }
     * 
     * @GetMapping("/api/user-info")
     * @ResponseBody
     * public UserDto getCurrentUser(Authentication auth) {
     *     // API endpoint para dados do usuário (retorna JSON)
     *     User user = (User) auth.getPrincipal();
     *     return new UserDto(user.getName(), user.getEmail());
     * }
     * 
     * @GetMapping("/admin")
     * @PreAuthorize("hasRole('ADMIN')")
     * public String adminPanel(Model model) {
     *     // Página só para administradores
     *     return "admin/panel";
     * }
     * 
     * TEMPLATES THYMELEAF CORRESPONDENTES:
     * 
     * dashboard.html:
     * <div th:if="${user}">
     *     <h1>Bem-vindo, <span th:text="${user}"></span>!</h1>
     *     <div th:if="${authorities}">
     *         <p>Suas permissões:</p>
     *         <ul>
     *             <li th:each="auth : ${authorities}" th:text="${auth.authority}"></li>
     *         </ul>
     *     </div>
     * </div>
     * 
     * NAVEGAÇÃO TÍPICA DA APLICAÇÃO:
     * 
     * 1. Usuário acessa / → Redirect para /auth/login (se não logado)
     * 2. Usuário faz login → Redirect para /dashboard (após sucesso)  
     * 3. Usuário navega pelo sistema → Sempre pode voltar ao /dashboard
     * 4. Usuário faz logout → Redirect para /auth/login
     * 
     * CONFIGURAÇÃO DE SEGURANÇA (SecurityConfig):
     * 
     * .authorizeHttpRequests(auth -> auth
     *     .requestMatchers("/", "/auth/**", "/css/**").permitAll()  // Público
     *     .anyRequest().authenticated()                             // Protegido
     * )
     * .formLogin(form -> form
     *     .loginPage("/auth/login")
     *     .defaultSuccessUrl("/dashboard", true)  // Vai para dashboard após login
     * )
     */
}