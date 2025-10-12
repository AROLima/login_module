package com.login.login.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.login.login.domain.User;
import com.login.login.repo.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createUser(String email, String password, String name) {
        // Verificar se usuário já existe
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email já cadastrado");
        }

        // Criar novo usuário
        User user = User.ofnew(email, passwordEncoder.encode(password), name);
        return userRepository.save(user);
    }

    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}