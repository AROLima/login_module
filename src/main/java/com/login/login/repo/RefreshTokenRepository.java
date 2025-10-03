package com.login.login.repo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.login.login.domain.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    
}
