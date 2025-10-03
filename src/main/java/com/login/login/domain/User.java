package com.login.login.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;

import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) //construtor protegido para JPA
@AllArgsConstructor(access = AccessLevel.PRIVATE) //construtor privado para uso interno
@Builder (toBuilder = true) //builder para facilitar criação de objetos
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true) //email é unico
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;
    
    //builder default para novo usuário ser habilitado por padrão (true) 
    @Builder.Default
    private boolean enabled = true;
    public static User ofnew(String email, String password, String name) {
        return User.builder()
            .email(email)
            .password(password)
            .name(name)
            .enabled(true)
            .build();
    }

    //implementação dos métodos da interface UserDetails
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        //MVP: Todos os  usuários são ROLE_USER
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
    
    @Override
    public String getUsername() {
        return email;
    }
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
