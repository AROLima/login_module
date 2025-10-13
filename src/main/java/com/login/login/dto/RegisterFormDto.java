// Pacote dto - Data Transfer Objects para transferir dados entre camadas
package com.login.login.dto;

// Importações de validação Jakarta (Bean Validation)
import jakarta.validation.constraints.*;  // Anotações de validação automática

/**
 * DTO PARA FORMULÁRIO DE REGISTRO/CADASTRO
 * 
 * Usado quando usuário cria nova conta no sistema.
 * 
 * VALIDAÇÕES IMPLEMENTADAS:
 * - Email válido e obrigatório
 * - Senha com mínimo 8 caracteres
 * - Nome obrigatório
 * - Confirmação de senha (deve ser igual à senha)
 * 
 * RECORD vs CLASS:
 * - Record: dados imutáveis, getters automáticos, perfeito para DTOs
 * - Class: mais flexível, permite métodos customizados
 * 
 * VANTAGENS DO RECORD:
 * - Menos código (sem boilerplate)
 * - Imutável por padrão (thread-safe)
 * - Semântica clara: "isso é apenas dados"
 */
public record RegisterFormDto(
    /**
     * EMAIL DO USUÁRIO
     * 
     * @Email - valida formato de email
     *         Regex interno: RFC 5322 compliant
     *         Exemplos válidos: "user@domain.com", "first.last@subdomain.domain.co"
     * 
     * @NotBlank - não pode ser nulo, vazio ou só espaços em branco
     * 
     * IMPORTANTE: Email será usado como username único no sistema
     */
    @Email 
    @NotBlank 
    String email,
    
    /**
     * SENHA DO USUÁRIO
     * 
     * @NotBlank - campo obrigatório
     * @Size(min = 8) - mínimo 8 caracteres
     * 
     * SEGURANÇA:
     * - Será hasheada com BCrypt antes de salvar
     * - BCrypt inclui salt automático
     * - Nunca armazenamos senha em plain text
     * 
     * PODERIA MELHORAR COM:
     * @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", 
     *          message = "Senha deve ter: maiúscula, minúscula e número")
     */
    @NotBlank 
    @Size(min = 8) 
    String password,
    
    /**
     * NOME COMPLETO DO USUÁRIO
     * 
     * @NotBlank - campo obrigatório, não pode ser só espaços
     * 
     * OBSERVAÇÕES:
     * - Usado para exibição na interface
     * - Pode ser alterado após cadastro
     * - Não precisa ser único (diferente do email)
     */
    @NotBlank 
    String name,
    
    /**
     * CONFIRMAÇÃO DA SENHA
     * 
     * @NotBlank - campo obrigatório
     * @Size(min = 8) - mesmo tamanho mínimo da senha
     * 
     * VALIDAÇÃO CUSTOMIZADA NECESSÁRIA:
     * Esta validação só verifica tamanho, mas precisamos validar se:
     * password.equals(confirmPassword)
     * 
     * Isso é feito no Service ou com validação customizada.
     */
    @NotBlank 
    @Size(min = 8) 
    String confirmPassword
) {
    /*
     * MÉTODOS AUTOMÁTICOS DO RECORD:
     * 
     * CONSTRUTOR:
     * public RegisterFormDto(String email, String password, String name, String confirmPassword)
     * 
     * GETTERS (sem prefixo "get"):
     * public String email() { return email; }
     * public String password() { return password; }  
     * public String name() { return name; }
     * public String confirmPassword() { return confirmPassword; }
     * 
     * OUTROS:
     * public boolean equals(Object obj) { ... }
     * public int hashCode() { ... }
     * public String toString() { ... }
     * 
     * USO NO CONTROLLER:
     * 
     * @PostMapping("/register")
     * public String register(@Valid RegisterFormDto form, BindingResult result) {
     *     // 1. Validações automáticas (anotações) já foram executadas
     *     
     *     // 2. Verificar erros de validação
     *     if (result.hasErrors()) {
     *         return "register-form"; // volta ao formulário com erros
     *     }
     *     
     *     // 3. Validação customizada (senhas iguais)
     *     if (!form.password().equals(form.confirmPassword())) {
     *         result.rejectValue("confirmPassword", "password.mismatch", "Senhas não coincidem");
     *         return "register-form";
     *     }
     *     
     *     // 4. Processar cadastro
     *     userService.createUser(form);
     *     return "redirect:/login?registered=true";
     * }
     * 
     * USO EM TEMPLATES (Thymeleaf):
     * 
     * <form th:object="${registerFormDto}" method="post">
     *     <input type="email" th:field="*{email}" />
     *     <span th:if="${#fields.hasErrors('email')}" th:errors="*{email}"></span>
     *     
     *     <input type="password" th:field="*{password}" />
     *     <span th:if="${#fields.hasErrors('password')}" th:errors="*{password}"></span>
     *     
     *     <input type="text" th:field="*{name}" />
     *     <span th:if="${#fields.hasErrors('name')}" th:errors="*{name}"></span>
     *     
     *     <input type="password" th:field="*{confirmPassword}" />
     *     <span th:if="${#fields.hasErrors('confirmPassword')}" th:errors="*{confirmPassword}"></span>
     *     
     *     <button type="submit">Cadastrar</button>
     * </form>
     * 
     * VALIDAÇÕES ADICIONAIS QUE PODERÍAMOS IMPLEMENTAR:
     * 
     * 1. Validação customizada de classe:
     * @ValidPasswordMatch
     * public record RegisterFormDto(...) {}
     * 
     * 2. Validação de email único:
     * @UniqueEmail
     * String email
     * 
     * 3. Complexidade de senha:
     * @StrongPassword
     * String password
     */
}
