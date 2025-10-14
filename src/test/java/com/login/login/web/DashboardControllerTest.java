package com.login.login.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para DashboardController
 * 
 * Testa autorização, redirecionamentos e páginas protegidas
 */
@WebMvcTest(DashboardController.class)
@Import({TestSecurityConfig.class})
@DisplayName("Dashboard Controller Integration Tests")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Deve exibir dashboard para usuário autenticado")
    @WithMockUser(username = "test@example.com", roles = "USER")
    void shouldDisplayDashboardForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    @DisplayName("Deve redirecionar usuário não autenticado para login")
    @WithAnonymousUser
    void shouldRedirectUnauthenticatedUserToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())  // Spring Security redireciona para página de login
                .andExpect(redirectedUrlPattern("**/auth/login"));
    }

    @Test
    @DisplayName("Deve redirecionar root para dashboard quando autenticado")
    @WithMockUser(username = "test@example.com", roles = "USER")
    void shouldRedirectRootToDashboardWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("Deve redirecionar root para login quando não autenticado")
    @WithAnonymousUser
    void shouldRedirectRootToLoginWhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())  // Spring Security redireciona para página de login
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    @DisplayName("Deve exibir informações do usuário no dashboard")
    @WithMockUser(username = "john@example.com", roles = "USER")
    void shouldDisplayUserInfoInDashboard() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    @DisplayName("Deve aceitar requisições GET apenas")
    @WithMockUser(username = "test@example.com", roles = "USER")
    void shouldAcceptGETRequestsOnly() throws Exception {
        // GET deve funcionar
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk());

        // POST deve retornar Method Not Allowed
        mockMvc.perform(post("/dashboard"))
                .andExpect(status().isMethodNotAllowed());

        // PUT deve retornar Method Not Allowed
        mockMvc.perform(put("/dashboard"))
                .andExpect(status().isMethodNotAllowed());

        // DELETE deve retornar Method Not Allowed
        mockMvc.perform(delete("/dashboard"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("Deve lidar com diferentes tipos de usuários autenticados")
    @WithMockUser(username = "admin@example.com", roles = {"USER", "ADMIN"})
    void shouldHandleDifferentTypesOfAuthenticatedUsers() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    @DisplayName("Deve manter estado da sessão entre requests")
    @WithMockUser(username = "session@example.com", roles = "USER")
    void shouldMaintainSessionStateBetweenRequests() throws Exception {
        // Primeira requisição
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));

        // Segunda requisição - deve manter autenticação
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }
}
