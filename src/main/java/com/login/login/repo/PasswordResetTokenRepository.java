package com.login.login.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.login.login.domain.PasswordResetToken;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    
}
