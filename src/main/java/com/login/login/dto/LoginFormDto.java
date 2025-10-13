// Pacote dto - Data Transfer Objects (objetos para transferir dados)
package com.login.login.dto;

// Importações de validação Jakarta
import jakarta.validation.constraints.*;  // Anotações para validar dados

/**
 * DTO PARA FORMULÁRIO DE LOGIN
 * 
 * DTO = Data Transfer Object
 * - Objetos simples usados apenas para transportar dados
 * - Não contêm lógica de negócio
 * - Usados entre camadas (Controller ↔ Service)
 * 
 * RECORD (Java 14+):
 * - Classe especial para dados imutáveis  
 * - Gera automaticamente: constructor, getters, equals, hashCode, toString
 * - Perfeito para DTOs pois são apenas "dados"
 * 
 * VALIDAÇÃO:
 * - Usa Bean Validation (Jakarta Validation)
 * - Validação automática no Spring com @Valid
 * - Erros são capturados e retornados como mensagens
 */
public record LoginFormDto(
    /**
     * EMAIL DO USUÁRIO
     * 
     * @Email - valida se está no formato de email válido
     *         Exemplos válidos: "user@example.com", "test.email+tag@domain.co"
     *         Exemplos inválidos: "email", "email@", "@domain.com"
     * 
     * @NotBlank - valida que não está vazio, nulo ou só espaços
     *            Diferenças:
     *            - @NotNull: não pode ser null
     *            - @NotEmpty: não pode ser null nem vazio ("")
     *            - @NotBlank: não pode ser null, vazio ou só espaços ("   ")
     */
    @Email 
    @NotBlank 
    String email,
    
    /**
     * SENHA DO USUÁRIO
     * 
     * @NotBlank - senha não pode estar vazia
     * 
     * NOTA: Não validamos complexidade da senha aqui pois:
     * - Na criação de conta já foi validada
     * - No login apenas verificamos se foi fornecida
     * - Complexidade é responsabilidade do cadastro
     */
    @NotBlank 
    String password
) {
    /*
     * MÉTODOS AUTOMATICAMENTE GERADOS PELO RECORD:
     * 
     * CONSTRUTOR:
     * public LoginFormDto(String email, String password) {
     *     this.email = email;
     *     this.password = password;
     * }
     * 
     * GETTERS (com nomes simples, sem "get"):
     * public String email() { return email; }
     * public String password() { return password; }
     * 
     * OUTROS MÉTODOS:
     * public boolean equals(Object obj) { ... }
     * public int hashCode() { ... }
     * public String toString() { return "LoginFormDto[email=..., password=***]"; }
     * 
     * USO NO CONTROLLER:
     * 
     * @PostMapping("/login")
     * public String login(@Valid LoginFormDto loginForm, BindingResult result) {
     *     if (result.hasErrors()) {
     *         // Tratar erros de validação
     *         return "login-form";
     *     }
     *     
     *     String email = loginForm.email();      // getter automático
     *     String senha = loginForm.password();   // getter automático
     *     
     *     // processar login...
     * }
     * 
     * USO EM TEMPLATES (Thymeleaf):
     * 
     * <input type="email" th:field="*{email}" />
     * <span th:if="${#fields.hasErrors('email')}" th:errors="*{email}"></span>
     * 
     * <input type="password" th:field="*{password}" />
     * <span th:if="${#fields.hasErrors('password')}" th:errors="*{password}"></span>
     */
}