package com.login.login.jwt;
import java.sql.Date;
import java.time.Instant;

import javax.crypto.SecretKey;

import com.jetbrains.exported.JBRApi.Service;
import com.login.login.domain.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;

@Service
public class JwtService {
    private final SecretKey key; // inicalização da chave 
    private final String issuer; // inicialização do emissor
    private final Long accessTtlMin; // inicialização do tempo de vida do token de acesso em minutos

    public JwtService(
        @Value("${app.jwt.secret}") String base64secret, // chave secreta em base64
        @Value("${app.jwt.issuer}") String issuer,  // quem emite o token
        @Value("${app.jwt.access-token.ttl-min}") Long accessTtlMin
    ){
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64secret)); // decodifica a chave base64 e cria a chave HMAC-SHA
        this.issuer = issuer; // define o emissor
        this.accessTtlMin = accessTtlMin; // define o tempo de vida do token de acesso
    }
    public String createAcessToken(User user) {
        Instant now = Instant.now(); // tempo atual
        return Jwts.builder()
        .subject(user.getId().toString())
        .claim("email", user.getEmail())
        .claim("name", user.getName())
        .issuer(issuer)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(accessTtlMin * 60)))
        .signWith(key, Jwts.SIG.HS256) // assinatura com a chave e algoritmo HS256
        .compact(); // gera o token
    }

    public Long subjectToUserId(String jwt){
        var jws = Jwts.parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(jwt);
        return Long.valueOf(jws.getPayload().getSubject());
    }
    
}
