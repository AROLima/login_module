package com.login.login.repo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.login.login.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {
    //busca usuário por email
    Optional<User> findByEmail(String email);    
}
