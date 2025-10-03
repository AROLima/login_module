package com.login.login.domain;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor (access = AccessLevel.PRIVATE)
@NoArgsConstructor (access = AccessLevel.PROTECTED)
@Builder (toBuilder = true)
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(optional = false, fetch= FetchType.LAZY)
    private User user;

    @Column(nullable = false)
    private Instant expiresAt; //timestamp em millis

    @Builder.Default
    @Column(nullable = false)
    private boolean used = false; //se o token já foi usado
}
