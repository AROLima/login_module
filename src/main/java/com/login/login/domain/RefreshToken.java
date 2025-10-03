package com.login.login.domain;

import java.time.Instant;

import jakarta.persistence.*;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "refresh_token")
@Getter
@Setter
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch= FetchType.LAZY)
    private User user;

    @Column(nullable = false, unique = true, length = 200)
    private String tokenHash; // guarda o hash do refresh não o plaintext

    @Column (nullable = false)
    private Instant expiresAt; //validade do token

    @Column (nullable = false)
    private boolean revoked = false; //se o token foi revogado

}
