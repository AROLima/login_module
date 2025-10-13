// Pacote dto - Data Transfer Objects para comunicação entre camadas
package com.login.login.dto;

// Importações de validação Jakarta
import jakarta.validation.constraints.*;  // Anotações para validação automática de dados

/**
 * DTO PARA FORMULÁRIO DE REDEFINIÇÃO DE SENHA
 * 
 * Usado quando usuário define nova senha após clicar no link de reset enviado por email.
 * 
 * FLUXO COMPLETO DE RESET:
 * 1. Usuário esquece senha → clica "Esqueci minha senha"
 * 2. Sistema gera token único → envia por email
 * 3. Usuário clica no link: /reset-password?token=abc123
 * 4. Sistema valida token (existe? não expirou? não foi usado?)
 * 5. Se válido, mostra formulário com este DTO
 * 6. Usuário digita nova senha → submete formulário
 * 7. Sistema valida nova senha → atualiza no banco → marca token como usado
 * 
 * SIMPLICIDADE:
 * - Apenas um campo: nova senha
 * - Token vem pela URL (parâmetro GET)
 * - Confirmação de senha poderia ser adicionada para UX melhor
 */
public record ResetFormDto(
    /**
     * NOVA SENHA DO USUÁRIO
     * 
     * @NotBlank - campo obrigatório, não pode ser vazio ou só espaços
     *            Validações:
     *            - null ❌
     *            - "" ❌ (string vazia)
     *            - "   " ❌ (só espaços)
     *            - "senha123" ✅
     * 
     * @Size(min = 8) - mínimo 8 caracteres
     *                 Validações:
     *                 - "123" ❌ (só 3 caracteres)
     *                 - "senha12" ✅ (8 caracteres)
     *                 - "minhasenhasegura123" ✅ (21 caracteres)
     * 
     * SEGURANÇA:
     * - Nova senha será hasheada com BCrypt
     * - Salt automático incluído no hash
     * - Hash anterior será sobrescrito (senha antiga fica inválida)
     * 
     * MELHORIAS POSSÍVEIS:
     * - Validação de complexidade (maiúscula, minúscula, número, símbolo)
     * - Verificar se não é igual à senha anterior
     * - Lista de senhas fracas proibidas
     */
    @NotBlank 
    @Size(min = 8) 
    String newpassword
) {
    /*
     * MÉTODOS AUTOMATICAMENTE GERADOS PELO RECORD:
     * 
     * CONSTRUTOR:
     * public ResetFormDto(String newpassword) {
     *     this.newpassword = newpassword;
     * }
     * 
     * GETTER (sem prefixo "get"):
     * public String newpassword() { 
     *     return newpassword; 
     * }
     * 
     * MÉTODOS HERDADOS DE Object:
     * public boolean equals(Object obj) { ... }
     * public int hashCode() { ... }
     * public String toString() { return "ResetFormDto[newpassword=***]"; }
     * 
     * USO NO CONTROLLER:
     * 
     * @GetMapping("/reset-password")
     * public String showResetForm(@RequestParam String token, Model model) {
     *     // 1. Validar token
     *     if (!passwordResetService.isValidToken(token)) {
     *         return "redirect:/login?error=invalid-token";
     *     }
     *     
     *     // 2. Adicionar DTO vazio no model para o formulário
     *     model.addAttribute("resetFormDto", new ResetFormDto(""));
     *     model.addAttribute("token", token);
     *     
     *     return "reset-password-form";
     * }
     * 
     * @PostMapping("/reset-password")
     * public String processReset(@RequestParam String token,
     *                           @Valid ResetFormDto form,
     *                           BindingResult result) {
     *     
     *     // 1. Verificar erros de validação
     *     if (result.hasErrors()) {
     *         return "reset-password-form"; // volta ao form com erros
     *     }
     *     
     *     // 2. Processar reset
     *     boolean success = passwordResetService.resetPassword(token, form.newpassword());
     *     
     *     if (success) {
     *         return "redirect:/login?reset=success";
     *     } else {
     *         return "redirect:/login?error=reset-failed";
     *     }
     * }
     * 
     * USO EM TEMPLATE (Thymeleaf):
     * 
     * <form th:object="${resetFormDto}" method="post" th:action="@{/reset-password(token=${token})}">
     *     <div>
     *         <label>Nova Senha:</label>
     *         <input type="password" th:field="*{newpassword}" />
     *         <span th:if="${#fields.hasErrors('newpassword')}" 
     *               th:errors="*{newpassword}" 
     *               class="error">
     *         </span>
     *     </div>
     *     
     *     <button type="submit">Redefinir Senha</button>
     * </form>
     * 
     * MELHORIAS POSSÍVEIS PARA O DTO:
     * 
     * 1. Adicionar confirmação de senha:
     * 
     * public record ResetFormDto(
     *     @NotBlank @Size(min = 8) String newpassword,
     *     @NotBlank @Size(min = 8) String confirmPassword
     * ) {
     *     // Validação customizada no service ou com anotação customizada
     * }
     * 
     * 2. Validação de complexidade:
     * 
     * public record ResetFormDto(
     *     @NotBlank
     *     @Size(min = 8)
     *     @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&].*$",
     *              message = "Senha deve conter: maiúscula, minúscula, número e símbolo")
     *     String newpassword
     * ) {}
     * 
     * 3. Com token embutido (mais seguro):
     * 
     * public record ResetFormDto(
     *     @NotBlank String token,
     *     @NotBlank @Size(min = 8) String newpassword
     * ) {}
     */
}
