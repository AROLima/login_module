package com.login.login.web;

import com.login.login.dto.LoginFormDto;
import com.login.login.dto.RegisterFormDto;
import com.login.login.dto.ResetFormDto;
import com.login.login.service.PasswordResetService;
import com.login.login.service.UserService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

/** Controllers para páginas de autenticação Thymeleaf. */
@Controller 
@RequestMapping("/auth")
public class AuthPageController {
  
  private final UserService userService;
  private final PasswordResetService passwordResetService;

  public AuthPageController(UserService userService, PasswordResetService passwordResetService) {
    this.userService = userService;
    this.passwordResetService = passwordResetService;
  }
  
  @GetMapping("/login")
  public String loginPage(Model m, Authentication auth) {
    if (auth != null) return "redirect:/dashboard";
    m.addAttribute("form", new LoginFormDto(null, null));
    return "auth/login";
  }

  @GetMapping("/register")
  public String registerPage(Model m) {
    m.addAttribute("form", new RegisterFormDto(null, null, null, null));
    return "auth/register";
  }

  @GetMapping("/forgot")
  public String forgotPage() { 
    return "auth/forgot"; 
  }

  @GetMapping("/reset/{token}")
  public String resetPage(@PathVariable String token, Model m) {
    m.addAttribute("token", token);
    m.addAttribute("form", new ResetFormDto(null));
    return "auth/reset";
  }

  // ========== POST METHODS ==========

  @PostMapping("/register")
  public String registerPost(@Valid @ModelAttribute("form") RegisterFormDto form, 
                           BindingResult result, 
                           Model model,
                           RedirectAttributes redirectAttributes) {
    try {
      // Validar se senhas coincidem
      if (!form.password().equals(form.confirmPassword())) {
        result.rejectValue("confirmPassword", "error.password", "Senhas não coincidem");
      }

      if (result.hasErrors()) {
        return "auth/register";
      }

      // Criar usuário
      userService.createUser(form.email(), form.password(), form.name());
      
      redirectAttributes.addFlashAttribute("success", "Usuário criado com sucesso! Faça login.");
      return "redirect:/auth/login";
      
    } catch (IllegalArgumentException e) {
      result.rejectValue("email", "error.email", e.getMessage());
      return "auth/register";
    } catch (Exception e) {
      model.addAttribute("error", "Erro interno do servidor");
      return "auth/register";
    }
  }

  @PostMapping("/forgot")
  public String forgotPost(@RequestParam String email, 
                         Model model,
                         RedirectAttributes redirectAttributes) {
    try {
      if (email == null || email.trim().isEmpty()) {
        model.addAttribute("error", "Email é obrigatório");
        return "auth/forgot";
      }

      // Sempre mostra sucesso por segurança (não revela se email existe)
      passwordResetService.request(email.trim());
      
      redirectAttributes.addFlashAttribute("success", 
        "Se o email existir, você receberá um link para redefinir sua senha.");
      return "redirect:/auth/forgot";
      
    } catch (Exception e) {
      model.addAttribute("error", "Erro interno do servidor");
      return "auth/forgot";
    }
  }

  @PostMapping("/reset/{token}")
  public String resetPost(@PathVariable String token,
                        @Valid @ModelAttribute("form") ResetFormDto form,
                        BindingResult result,
                        Model model,
                        RedirectAttributes redirectAttributes) {
    try {
      if (result.hasErrors()) {
        model.addAttribute("token", token);
        return "auth/reset";
      }

      // Resetar senha
      passwordResetService.reset(token, form.newpassword());
      
      redirectAttributes.addFlashAttribute("success", 
        "Senha redefinida com sucesso! Faça login com sua nova senha.");
      return "redirect:/auth/login";
      
    } catch (IllegalArgumentException e) {
      model.addAttribute("token", token);
      model.addAttribute("error", e.getMessage());
      return "auth/reset";
    } catch (Exception e) {
      model.addAttribute("token", token);
      model.addAttribute("error", "Erro interno do servidor");
      return "auth/reset";
    }
  }
}