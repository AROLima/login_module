// Pacote jwt - serviços relacionados a JSON Web Tokens
package com.login.login.jwt;

// Importações Java
import java.sql.Date;      // Para compatibilidade com JWT library (java.util.Date)
import java.time.Instant;  // Para trabalhar com timestamps UTC

// Importação criptografia
import javax.crypto.SecretKey;  // Chave secreta para assinatura HMAC

// Importações do domínio
import com.login.login.domain.User;  // Entidade usuário

// Importações JWT (JJWT library)
import io.jsonwebtoken.*;                    // Classes principais JWT
import io.jsonwebtoken.io.Decoders;          // Decodificador Base64
import io.jsonwebtoken.security.Keys;        // Gerador de chaves criptográficas

// Importações Spring
import org.springframework.beans.factory.annotation.Value;  // Injeção de valores de configuração
import org.springframework.stereotype.Service;              // Marca como componente de serviço

/**
 * SERVIÇO JWT (JSON WEB TOKEN)
 * 
 * Responsável por criar e validar tokens JWT para autenticação stateless.
 * 
 * O QUE É JWT:
 * - Padrão RFC 7519 para tokens de acesso
 * - Composto por: Header.Payload.Signature
 * - Stateless (não precisa armazenar no servidor)
 * - Self-contained (contém todas as informações necessárias)
 * 
 * ESTRUTURA DO TOKEN:
 * eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0IiwiZW1haWwiOiJ1c2VyQGV4YW1wbGUuY29tIn0.signature
 *        ↑                           ↑                                    ↑
 *    HEADER                     PAYLOAD                             SIGNATURE
 *    {"alg":"HS256"}           {"sub":"1234","email":"user@..."}   HMAC-SHA256
 * 
 * VANTAGENS JWT:
 * - Stateless (escala horizontalmente)
 * - Cross-domain (CORS friendly)
 * - Mobile friendly
 * - Payload customizável
 * 
 * DESVANTAGENS JWT:
 * - Não pode ser revogado facilmente
 * - Tamanho maior que session ID
 * - Dados sensíveis ficam no cliente
 */
@Service  // Componente Spring gerenciado pelo container IoC
public class JwtService {
    
    /**
     * CHAVE SECRETA PARA ASSINATURA
     * 
     * SecretKey - chave simétrica HMAC-SHA256
     * - Mesma chave assina e valida tokens
     * - Deve ser mantida em segredo absoluto
     * - Mínimo 256 bits (32 bytes) para HS256
     * 
     * SEGURANÇA CRÍTICA:
     * - Se chave vazar, todos os tokens ficam comprometidos
     * - Usar variável de ambiente em produção
     * - Rotar chave periodicamente
     */
    private final SecretKey key; 

    /**
     * EMISSOR DO TOKEN (ISSUER)
     * 
     * Identifica quem criou o token.
     * Usado para validar origem em sistemas distribuídos.
     * 
     * Exemplos:
     * - "https://meuapp.com"
     * - "auth-service-v1"
     * - "my-application"
     */
    private final String issuer; 

    /**
     * TEMPO DE VIDA DO ACCESS TOKEN (TTL)
     * 
     * Tempo em minutos até o token expirar.
     * 
     * RECOMENDAÇÕES:
     * - Access Token: 15-30 minutos (vida curta)
     * - Refresh Token: 7-30 dias (vida longa) 
     * 
     * BALANÇO SEGURANÇA vs UX:
     * - Muito curto: usuário faz login frequentemente
     * - Muito longo: risco se token for roubado
     */
    private final Long accessTtlMin;

    /**
     * CONSTRUTOR COM INJEÇÃO DE CONFIGURAÇÃO
     * 
     * @Value injeta valores do application.yml
     * 
     * Exemplo de configuração:
     * app:
     *   jwt:
     *     secret: "base64-encoded-secret-key-here"
     *     issuer: "https://meuapp.com"  
     *     access-token:
     *       ttl-min: 30
     * 
     * @param base64secret Chave secreta codificada em Base64
     * @param issuer Identificador do emissor dos tokens
     * @param accessTtlMin Tempo de vida em minutos
     */
    public JwtService(
        @Value("${app.jwt.secret}") String base64secret,
        @Value("${app.jwt.issuer}") String issuer,
        @Value("${app.jwt.access-token.ttl-min}") Long accessTtlMin
    ){
        // DECODIFICAR E CRIAR CHAVE CRIPTOGRÁFICA
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64secret));
        //         ↑                  ↑
        //    Cria chave HMAC    Decodifica Base64
        //
        // Processo:
        // 1. Base64: "c2VjcmV0a2V5MTIzNDU2Nzg5MA==" 
        // 2. Decode: bytes da chave secreta
        // 3. HMAC: chave para algoritmo HS256

        this.issuer = issuer;
        this.accessTtlMin = accessTtlMin;
    }

    /**
     * CRIAR ACCESS TOKEN PARA USUÁRIO
     * 
     * Gera token JWT com informações do usuário autenticado.
     * 
     * CLAIMS INCLUÍDOS:
     * - sub (subject): ID do usuário
     * - email: email do usuário
     * - name: nome do usuário
     * - iss (issuer): emissor do token
     * - iat (issued at): quando foi emitido
     * - exp (expiration): quando expira
     * 
     * @param user Usuário autenticado
     * @return String Token JWT assinado
     */
    public String createAcessToken(User user) {
        Instant now = Instant.now(); // Momento atual (UTC)
        
        return Jwts.builder()
            // SUBJECT (sub) - identificador principal (ID do usuário)
            .subject(user.getId().toString())  // "1234"
            
            // CLAIMS CUSTOMIZADOS - dados úteis para a aplicação
            .claim("email", user.getEmail())   // "user@example.com"
            .claim("name", user.getName())     // "João Silva"
            
            // ISSUER (iss) - quem emitiu o token
            .issuer(issuer)  // "https://meuapp.com"
            
            // ISSUED AT (iat) - quando foi criado
            .issuedAt(Date.from(now))  // Timestamp atual
            
            // EXPIRATION (exp) - quando expira  
            .expiration(Date.from(now.plusSeconds(accessTtlMin * 60)))
            //                   ↑
            //              Minutos → Segundos
            
            // ASSINATURA - garante integridade e autenticidade
            .signWith(key, Jwts.SIG.HS256)  // HMAC-SHA256 com nossa chave
            
            // COMPACTAR - gerar string final do token
            .compact();
            //  ↑
            // Resultado: "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0Ij0.signature"
    }

    /**
     * EXTRAIR USER ID DO TOKEN JWT
     * 
     * Decodifica token e extrai o subject (ID do usuário).
     * 
     * PROCESSO:
     * 1. Parser verifica assinatura com nossa chave
     * 2. Se válida, extrai payload
     * 3. Pega subject e converte para Long
     * 
     * VALIDAÇÕES AUTOMÁTICAS:
     * - Assinatura válida
     * - Token não expirado
     * - Formato correto
     * 
     * @param jwt Token JWT recebido do cliente
     * @return Long ID do usuário
     * @throws JwtException se token inválido/expirado
     */
    public Long subjectToUserId(String jwt){
        // PARSER COM VALIDAÇÃO
        var jws = Jwts.parser()
            .verifyWith(key)        // Verifica assinatura com nossa chave
            .build()                // Constrói parser
            .parseSignedClaims(jwt); // Decodifica e valida token
        //  ↑
        // JWS = JSON Web Signature (JWT assinado)

        // EXTRAIR SUBJECT E CONVERTER PARA ID
        return Long.valueOf(jws.getPayload().getSubject());
        //                  ↑             ↑
        //              Payload      Subject claim
    }
    
    /*
     * MÉTODOS ADICIONAIS QUE PODERÍAMOS IMPLEMENTAR:
     * 
     * public boolean isTokenValid(String jwt) {
     *     try {
     *         Jwts.parser().verifyWith(key).build().parseSignedClaims(jwt);
     *         return true;
     *     } catch (JwtException e) {
     *         return false;
     *     }
     * }
     * 
     * public Claims extractClaims(String jwt) {
     *     return Jwts.parser()
     *         .verifyWith(key)
     *         .build()
     *         .parseSignedClaims(jwt)
     *         .getPayload();
     * }
     * 
     * public String extractEmail(String jwt) {
     *     return extractClaims(jwt).get("email", String.class);
     * }
     * 
     * public String extractName(String jwt) {
     *     return extractClaims(jwt).get("name", String.class);
     * }
     * 
     * public boolean isTokenExpired(String jwt) {
     *     return extractClaims(jwt).getExpiration().before(new Date());
     * }
     * 
     * public String createRefreshToken(User user) {
     *     Instant now = Instant.now();
     *     return Jwts.builder()
     *         .subject(user.getId().toString())
     *         .issuer(issuer)
     *         .issuedAt(Date.from(now))
     *         .expiration(Date.from(now.plusDays(30))) // 30 dias
     *         .signWith(key, Jwts.SIG.HS256)
     *         .compact();
     * }
     * 
     * CONFIGURAÇÃO RECOMENDADA (application.yml):
     * 
     * app:
     *   jwt:
     *     secret: ${JWT_SECRET:c2VjcmV0a2V5MTIzNDU2Nzg5MGFiY2RlZmdoaWprbG1ub3A=}
     *     issuer: ${JWT_ISSUER:https://meuapp.com}
     *     access-token:
     *       ttl-min: ${JWT_ACCESS_TTL:30}
     *     refresh-token:
     *       ttl-days: ${JWT_REFRESH_TTL:30}
     * 
     * GERANDO CHAVE SECRETA SEGURA:
     * 
     * import java.util.Base64;
     * import javax.crypto.KeyGenerator;
     * 
     * KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
     * keyGen.init(256); // 256 bits
     * SecretKey key = keyGen.generateKey();
     * String base64Key = Base64.getEncoder().encodeToString(key.getEncoded());
     * System.out.println("Chave segura: " + base64Key);
     */
}
