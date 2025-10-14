package com.login.login.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;

import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyString;

import com.login.login.service.PasswordResetService;
import com.login.login.service.UserService;
import com.login.login.domain.User;

/**
 * TESTES DE INTEGRAÇÃO PARA CONTROLLERS DE AUTENTICAÇÃO
 * 
 * Testa endpoints de login, registro e reset de senha.
 * 
 * @WebMvcTest carrega apenas beans relacionados ao MVC
 * @Import adiciona configuração de segurança para testes
 */
@WebMvcTest(controllers = {AuthPageController.class})
@Import(TestSecurityConfig.class)
@DisplayName("Auth Controllers Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @Test
    @DisplayName("Deve exibir página de login")
    @WithAnonymousUser
    void shouldShowLoginPage() throws Exception {
        mockMvc.perform(get("/auth/login"))
            .andExpect(status().isOk())
            .andExpect(view().name("auth/login"));
    }

    @Test
    @DisplayName("Deve exibir página de registro")
    @WithAnonymousUser
    void shouldShowRegisterPage() throws Exception {
        mockMvc.perform(get("/auth/register"))
            .andExpect(status().isOk())
            .andExpect(view().name("auth/register"));
    }

    @Test
    @DisplayName("Deve redirecionar usuário autenticado para dashboard")
    @WithMockUser
    void shouldRedirectAuthenticatedUserToDashboard() throws Exception {
        mockMvc.perform(get("/auth/login"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("Deve processar registro com dados válidos")
    @WithAnonymousUser
    void shouldProcessValidRegistration() throws Exception {
        // Simular que o UserService cria o usuário com sucesso
        when(userService.createUser(anyString(), anyString(), anyString()))
            .thenReturn(User.ofnew("test@example.com", "hashedpass", "Test User"));

        mockMvc.perform(post("/auth/register")
                .param("email", "test@example.com")
                .param("name", "Test User")
                .param("password", "password123")
                .param("confirmPassword", "password123"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    @DisplayName("Deve rejeitar registro com senhas diferentes")
    @WithAnonymousUser
    void shouldRejectRegistrationWithDifferentPasswords() throws Exception {
        mockMvc.perform(post("/auth/register")
                .param("email", "test@example.com")
                .param("name", "Test User")
                .param("password", "password123")
                .param("confirmPassword", "differentpassword"))
            .andExpect(status().isOk())
            .andExpect(view().name("auth/register"));
    }

    @Test
    @DisplayName("Deve exibir página de reset de senha")
    @WithAnonymousUser
    void shouldShowResetPage() throws Exception {
        mockMvc.perform(get("/auth/forgot"))
            .andExpect(status().isOk())
            .andExpect(view().name("auth/forgot"));
    }

    @Test
    @DisplayName("Deve processar solicitação de reset válida")
    @WithAnonymousUser
    void shouldProcessValidResetRequest() throws Exception {
        doNothing().when(passwordResetService).request(anyString());
        
        mockMvc.perform(post("/auth/forgot")
                .param("email", "test@example.com"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/auth/forgot"));
    }

    @Test
    @DisplayName("Deve validar email no reset de senha")
    @WithAnonymousUser
    void shouldValidateEmailInReset() throws Exception {
        mockMvc.perform(post("/auth/forgot")
                .param("email", ""))
            .andExpect(status().isOk())
            .andExpect(view().name("auth/forgot"));
    }



    @Test
    @DisplayName("Deve permitir acesso a recursos estáticos")
    void shouldAllowAccessToStaticResources() throws Exception {
        mockMvc.perform(get("/css/style.css"))
            .andExpect(status().isNotFound()); // 404 é ok, 403 seria problema de segurança
    }
}