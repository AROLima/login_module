package com.login.login.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.login.login.domain.PasswordResetToken;
import com.login.login.mail.MailService;
import com.login.login.repo.PasswordResetTokenRepository;
import com.login.login.repo.UserRepository;
import jakarta.transaction.Transactional;

@Service
public class PasswordResetService {
    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final MailService mail;

    @Value("${app.security.reset-token-expiration-minutes}")
    long ttl;

    // Constructor
    public PasswordResetService (UserRepository u, PasswordResetTokenRepository t, PasswordEncoder e, MailService m) {
        this.users = u;
        this.tokens = t;
        this.encoder = e;
        this.mail = m;
    }


    // Methods

    @Transactional
    public void request(String email) {
        var user = users.findByEmail(email).orElse(null);
        if (user == null) {
            return;
        }
        var prt = PasswordResetToken.builder()
        .token(UUID.randomUUID().toString())
        .user(user)
        .expiresAt(Instant.now().plusSeconds(ttl * 60))
        .build();
        tokens.save(prt);
        mail.sendResetEmail(user.getEmail(), prt.getToken());
    }
    
    @Transactional
    public void reset(String token, String newPassword){
        var prt = tokens.findByTokenAndUsedFalse(token).orElseThrow(() -> new IllegalArgumentException("Invalid token"));
        if (prt.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token expired");   
        }
        var user = prt.getUser();
        user.setPassword(encoder.encode(newPassword));
        prt.setUsed(true);
        
        // Salvar as alterações no banco
        users.save(user);
        tokens.save(prt);
    }
}
