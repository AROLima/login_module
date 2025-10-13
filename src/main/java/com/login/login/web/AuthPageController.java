// Pacote web - camada de apresentação (controllers)
package com.login.login.web;

// Importações dos DTOs (objetos para transferir dados)
import com.login.login.dto.LoginFormDto;       // DTO para formulário de login
import com.login.login.dto.RegisterFormDto;    // DTO para formulário de cadastro
import com.login.login.dto.ResetFormDto;       // DTO para formulário de reset de senha

// Importações dos serviços (lógica de negócio)
import com.login.login.service.PasswordResetService;  // Serviço de reset de senha
import com.login.login.service.UserService;           // Serviço de usuários

// Importações Spring Security
import org.springframework.security.core.Authentication;  // Interface para usuário autenticado

// Importações Spring MVC
import org.springframework.stereotype.Controller;                        // Marca como controller MVC
import org.springframework.ui.Model;                                     // Para passar dados para view
import org.springframework.validation.BindingResult;                     // Para capturar erros de validação
import org.springframework.web.bind.annotation.*;                        // Anotações de mapeamento HTTP
import org.springframework.web.servlet.mvc.support.RedirectAttributes;   // Para flash attributes em redirects

// Importação de validação
import jakarta.validation.Valid;  // Para validação automática de DTOs

/**
 * CONTROLLER DE PÁGINAS DE AUTENTICAÇÃO
 * 
 * Responsável por todas as páginas relacionadas à autenticação:
 * - Login (GET/POST)
 * - Registro/Cadastro (GET/POST)  
 * - Esqueci senha (GET/POST)
 * - Reset de senha (GET/POST)
 * 
 * PADRÃO MVC:
 * Controller → Service → Repository → Database
 *     ↓
 *   View (Thymeleaf templates)
 * 
 * ARQUITETURA SPRING MVC:
 * 1. DispatcherServlet recebe requisição
 * 2. HandlerMapping encontra o controller
 * 3. Controller processa requisição
 * 4. Controller retorna nome da view
 * 5. ViewResolver encontra template
 * 6. Template é renderizado com dados do Model
 * 
 * RESPONSABILIDADES:
 * - Receber requisições HTTP
 * - Validar dados de entrada
 * - Chamar serviços de negócio
 * - Preparar dados para a view
 * - Retornar nome da view ou redirect
 */
@Controller  // Marca como componente Spring MVC (não @RestController pois retorna views, não JSON)
@RequestMapping("/auth")  // Todas as rotas deste controller começam com /auth
public class AuthPageController {
  
    /**
     * DEPENDÊNCIAS INJETADAS
     * 
     * final = imutáveis após construção (thread-safe)
     * Injetadas via construtor (Dependency Injection)
     */
    private final UserService userService;                // Para criar usuários
    private final PasswordResetService passwordResetService;  // Para reset de senhas

    /**
     * CONSTRUTOR COM INJEÇÃO DE DEPENDÊNCIA
     * 
     * Spring automaticamente injeta as implementações dos serviços.
     * Não precisamos de @Autowired no construtor (recomendação atual).
     * 
     * @param userService Serviço para operações de usuário
     * @param passwordResetService Serviço para reset de senhas
     */
    public AuthPageController(UserService userService, PasswordResetService passwordResetService) {
        this.userService = userService;
        this.passwordResetService = passwordResetService;
    }
  
    /**
     * PÁGINA DE LOGIN (GET)
     * 
     * Exibe formulário de login para usuários não autenticados.
     * 
     * ROTA: GET /auth/login
     * 
     * LÓGICA:
     * - Se usuário já está logado → redireciona para dashboard
     * - Se não está logado → mostra formulário de login
     * 
     * @param m Model para passar dados para a view
     * @param auth Authentication (injetado automaticamente pelo Spring Security)
     *             - null se usuário não autenticado
     *             - objeto com dados se usuário autenticado
     * @return String nome da view ou redirect
     */
    @GetMapping("/login")
    public String loginPage(Model m, Authentication auth) {
        // VERIFICAR SE USUÁRIO JÁ ESTÁ AUTENTICADO
        if (auth != null) return "redirect:/dashboard";
        //     ↑                    ↑
        //  Já logado         Redireciona (HTTP 302)
        
        // PREPARAR FORMULÁRIO VAZIO PARA A VIEW
        m.addAttribute("form", new LoginFormDto(null, null));
        //             ↑            ↑
        //        Nome no template  DTO vazio
        
        // RETORNAR NOME DA VIEW
        return "auth/login";
        //      ↑
        // ViewResolver procurará: /templates/auth/login.html
    }

    /**
     * PÁGINA DE REGISTRO (GET)
     * 
     * Exibe formulário de cadastro para novos usuários.
     * 
     * ROTA: GET /auth/register
     * 
     * @param m Model para passar dados para a view
     * @return String nome da view
     */
    @GetMapping("/register")
    public String registerPage(Model m) {
        // PREPARAR FORMULÁRIO VAZIO PARA CADASTRO
        m.addAttribute("form", new RegisterFormDto(null, null, null, null));
        //             ↑                           ↑     ↑     ↑     ↑
        //       Nome no template              email pass name confirmPass
        
        return "auth/register";  // → /templates/auth/register.html
    }

    /**
     * PÁGINA "ESQUECI MINHA SENHA" (GET)
     * 
     * Exibe formulário para solicitar reset de senha.
     * 
     * ROTA: GET /auth/forgot
     * 
     * @return String nome da view
     */
    @GetMapping("/forgot")
    public String forgotPage() { 
        return "auth/forgot";  // → /templates/auth/forgot.html
        //      ↑
        // Não precisa de Model pois formulário só tem um campo (email)
    }

    /**
     * PÁGINA DE RESET DE SENHA (GET)
     * 
     * Exibe formulário para definir nova senha.
     * Acessada via link enviado por email.
     * 
     * ROTA: GET /auth/reset/{token}
     * Exemplo: GET /auth/reset/a1b2c3d4-e5f6-7890-abcd-ef1234567890
     * 
     * @param token String token único de reset vindo da URL
     * @param m Model para passar dados para a view
     * @return String nome da view
     */
    @GetMapping("/reset/{token}")
    public String resetPage(@PathVariable String token, Model m) {
        //                  ↑           ↑
        //            Anotação      Valor da URL
        
        // PASSAR TOKEN PARA A VIEW (será usado no formulário POST)
        m.addAttribute("token", token);
        
        // PREPARAR FORMULÁRIO VAZIO
        m.addAttribute("form", new ResetFormDto(null));
        //                                      ↑
        //                              Nova senha vazia
        
        return "auth/reset";  // → /templates/auth/reset.html
    }

    // ========== MÉTODOS POST - PROCESSAMENTO DE FORMULÁRIOS ==========

    /**
     * PROCESSAR CADASTRO DE USUÁRIO (POST)
     * 
     * Recebe dados do formulário de registro e cria novo usuário.
     * 
     * ROTA: POST /auth/register
     * 
     * VALIDAÇÕES APLICADAS:
     * 1. Validações automáticas do DTO (@Valid)
     * 2. Confirmação de senha
     * 3. Email único (via service)
     * 
     * FLUXO DE SUCESSO:
     * 1. Valida dados → 2. Cria usuário → 3. Redireciona para login
     * 
     * FLUXO DE ERRO:
     * 1. Encontra erro → 2. Volta para formulário com mensagem
     * 
     * @param form RegisterFormDto dados validados do formulário
     * @param result BindingResult erros de validação capturados
     * @param model Model para passar dados em caso de erro
     * @param redirectAttributes Para flash messages em redirects
     * @return String view ou redirect
     */
    @PostMapping("/register")
    public String registerPost(@Valid @ModelAttribute("form") RegisterFormDto form, 
                             BindingResult result, 
                             Model model,
                             RedirectAttributes redirectAttributes) {
        //               ↑                      ↑            ↑              ↑
        //         Valida DTO          Erros capturados  Dados p/ view  Flash messages
        
        try {
            // VALIDAÇÃO CUSTOMIZADA: SENHAS COINCIDEM
            if (!form.password().equals(form.confirmPassword())) {
                result.rejectValue("confirmPassword", "error.password", "Senhas não coincidem");
                //     ↑                ↑                 ↑                  ↑
                //   Campo        Código do erro    Categoria         Mensagem
            }

            // VERIFICAR SE HÁ ERROS DE VALIDAÇÃO
            if (result.hasErrors()) {
                return "auth/register";  // Volta para formulário com erros
            }

            // CRIAR USUÁRIO VIA SERVICE
            userService.createUser(form.email(), form.password(), form.name());
            //          ↑               ↑           ↑              ↑
            //      Service      Email único   Hash BCrypt    Nome completo
            
            // SUCESSO: FLASH MESSAGE E REDIRECT
            redirectAttributes.addFlashAttribute("success", "Usuário criado com sucesso! Faça login.");
            //                                      ↑              ↑
            //                              Tipo da mensagem  Texto exibido
            return "redirect:/auth/login";
            //      ↑
            // Redirect evita resubmissão do formulário (PRG pattern)
            
        } catch (IllegalArgumentException e) {
            // ERRO DE REGRA DE NEGÓCIO (ex: email já existe)
            result.rejectValue("email", "error.email", e.getMessage());
            //                  ↑         ↑              ↑
            //               Campo    Categoria     Mensagem do service
            return "auth/register";  // Volta para formulário com erro
            
        } catch (Exception e) {
            // ERRO INTERNO DO SERVIDOR
            model.addAttribute("error", "Erro interno do servidor");
            //                   ↑              ↑
            //              Chave no model   Mensagem genérica (não vaza detalhes)
            return "auth/register";
        }
    }

    /**
     * PROCESSAR SOLICITAÇÃO DE RESET (POST)
     * 
     * Recebe email e envia link de reset se usuário existe.
     * 
     * ROTA: POST /auth/forgot
     * 
     * SEGURANÇA - PROTEÇÃO CONTRA ENUMERAÇÃO:
     * - Sempre mostra mensagem de sucesso
     * - Não revela se email existe ou não no sistema
     * - Previne ataques para descobrir emails cadastrados
     * 
     * @param email String email informado no formulário
     * @param model Model para mostrar erros
     * @param redirectAttributes Para flash message de sucesso
     * @return String view ou redirect
     */
    @PostMapping("/forgot")
    public String forgotPost(@RequestParam String email, 
                           Model model,
                           RedirectAttributes redirectAttributes) {
        //                 ↑           ↑
        //            Parâmetro HTTP   Campo do form
        
        try {
            // VALIDAÇÃO BÁSICA: EMAIL INFORMADO
            if (email == null || email.trim().isEmpty()) {
                model.addAttribute("error", "Email é obrigatório");
                return "auth/forgot";  // Volta para formulário
            }

            // PROCESSAR SOLICITAÇÃO DE RESET
            // IMPORTANTE: Service não revela se email existe!
            passwordResetService.request(email.trim());
            //                            ↑
            //                    Remove espaços extras
            
            // MENSAGEM DE SUCESSO AMBÍGUA (SEGURANÇA)
            redirectAttributes.addFlashAttribute("success", 
                "Se o email existir, você receberá um link para redefinir sua senha.");
            //   ↑
            // Não confirma nem nega existência do email
            
            return "redirect:/auth/forgot";  // PRG pattern
            
        } catch (Exception e) {
            // ERRO INTERNO
            model.addAttribute("error", "Erro interno do servidor");
            return "auth/forgot";
        }
    }

    /**
     * PROCESSAR RESET DE SENHA (POST)
     * 
     * Recebe nova senha e token, valida e atualiza senha do usuário.
     * 
     * ROTA: POST /auth/reset/{token}
     * 
     * VALIDAÇÕES APLICADAS:
     * 1. Token válido e não expirado
     * 2. Token não usado anteriormente
     * 3. Nova senha atende critérios mínimos
     * 
     * SEGURANÇA:
     * - Token de uso único (marcado como usado após sucesso)
     * - Nova senha é hasheada com BCrypt
     * - Token tem expiração (configurável)
     * 
     * @param token String token único de reset vindo da URL
     * @param form ResetFormDto dados validados do formulário
     * @param result BindingResult erros de validação capturados
     * @param model Model para passar dados em caso de erro
     * @param redirectAttributes Para flash message de sucesso
     * @return String view ou redirect
     */
    @PostMapping("/reset/{token}")
    public String resetPost(@PathVariable String token,
                          @Valid @ModelAttribute("form") ResetFormDto form,
                          BindingResult result,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        //               ↑                      ↑            ↑              ↑
        //        Token da URL          Dados validados  Erros capturados  Flash messages
        
        try {
            // VERIFICAR ERROS DE VALIDAÇÃO DO FORMULÁRIO
            if (result.hasErrors()) {
                model.addAttribute("token", token);  // Repassar token para view
                return "auth/reset";  // Volta para formulário com erros
            }

            // PROCESSAR RESET DE SENHA VIA SERVICE
            passwordResetService.reset(token, form.newpassword());
            //                         ↑           ↑
            //                   Token único   Nova senha
            //
            // Service fará:
            // 1. Validar token (existe, não usado, não expirado)
            // 2. Hashear nova senha com BCrypt
            // 3. Atualizar senha do usuário
            // 4. Marcar token como usado
            
            // SUCESSO: FLASH MESSAGE E REDIRECT PARA LOGIN
            redirectAttributes.addFlashAttribute("success", 
                "Senha redefinida com sucesso! Faça login com sua nova senha.");
            return "redirect:/auth/login";  // Usuario vai fazer login com nova senha
            
        } catch (IllegalArgumentException e) {
            // ERRO DE VALIDAÇÃO DE NEGÓCIO (token inválido/expirado)
            model.addAttribute("token", token);      // Manter token na view
            model.addAttribute("error", e.getMessage());  // Mensagem específica do service
            return "auth/reset";  // Volta para formulário com erro
            
        } catch (Exception e) {
            // ERRO INTERNO DO SERVIDOR
            model.addAttribute("token", token);      // Manter token na view
            model.addAttribute("error", "Erro interno do servidor");  // Mensagem genérica
            return "auth/reset";
        }
    }
    
    /*
     * MÉTODOS ADICIONAIS QUE PODERÍAMOS IMPLEMENTAR:
     * 
     * @GetMapping("/logout")
     * public String logout(HttpServletRequest request, HttpServletResponse response) {
     *     // Logout customizado (limpar cookies, etc.)
     *     Authentication auth = SecurityContextHolder.getContext().getAuthentication();
     *     if (auth != null) {
     *         new SecurityContextLogoutHandler().logout(request, response, auth);
     *     }
     *     return "redirect:/auth/login?logout=true";
     * }
     * 
     * @GetMapping("/profile")
     * public String profile(@AuthenticationPrincipal User user, Model model) {
     *     model.addAttribute("user", user);
     *     return "auth/profile";
     * }
     * 
     * @PostMapping("/change-password")
     * public String changePassword(@AuthenticationPrincipal User user,
     *                            @RequestParam String currentPassword,
     *                            @RequestParam String newPassword,
     *                            Model model) {
     *     try {
     *         userService.changePassword(user.getId(), currentPassword, newPassword);
     *         model.addAttribute("success", "Senha alterada com sucesso!");
     *         return "auth/profile";
     *     } catch (Exception e) {
     *         model.addAttribute("error", e.getMessage());
     *         return "auth/profile";
     *     }
     * }
     * 
     * PADRÕES IMPLEMENTADOS NESTE CONTROLLER:
     * 
     * 1. POST-REDIRECT-GET (PRG):
     *    - POST processa dados → Redirect evita resubmissão → GET mostra resultado
     * 
     * 2. Flash Attributes:
     *    - Mensagens que sobrevivem ao redirect
     *    - redirectAttributes.addFlashAttribute()
     * 
     * 3. Model Attributes:
     *    - Dados para a view atual
     *    - model.addAttribute()
     * 
     * 4. Validation Pattern:
     *    - @Valid + BindingResult para capturar erros
     *    - Validação automática + validação customizada
     * 
     * 5. Exception Handling:
     *    - Try-catch por método
     *    - Mensagens específicas vs genéricas
     *    - Não vazar informações sensíveis
     * 
     * 6. Security Best Practices:
     *    - Não enumerar usuários existentes
     *    - Mensagens ambíguas em operações sensíveis
     *    - Validação tanto client-side quanto server-side
     */
}