package com.dishahara.security;


import com.dishahara.entities.Role;
import com.dishahara.entities.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.dialect.aggregate.H2AggregateSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Getter
@Setter
public class JwtService {

    private final SecretKey key;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;
    private final String issuer;

    public JwtService(
            @Value("${logging.security.jwt.secret}") String secretKey,
            @Value("${logging.security.jwt.access-ttl-second}") long accessTtlSeconds,
            @Value("${logging.security.jwt.refresh-ttl-second}") long refreshTtlSeconds ,
            @Value("${logging.security.jwt.issuer}") String issuer ) {

        if (secretKey == null || secretKey.length()<60) {
           throw  new IllegalArgumentException("Invalid JWT Secret Key");
        }
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessTtlSeconds=accessTtlSeconds;
        this.refreshTtlSeconds=refreshTtlSeconds;
        this.issuer=issuer;

    }

    //Generate -> Access Token
    public String generateToken(User user) {
        Instant now = Instant.now();
       List<String> roles= user.getRoles()==null? List.of() : user.getRoles().stream().map(Role::getName).toList();
       return Jwts.builder()
               .id(UUID.randomUUID().toString())
               .subject(user.getId().toString())
               .issuer(issuer)
               .issuedAt(Date.from(now))
               .expiration(Date.from(now.plusSeconds(accessTtlSeconds)))
               .claims(Map.of(
                       "email", user.getEmail(),
                       "roles",roles,
                       "typ","access"
               ))
               .signWith(key, SignatureAlgorithm.HS256)
               .compact();


    }

    //Refresh->Token
    public String refreshToken(User user, String jwtId) {
        Instant now = Instant.now();
        List<String> roles= user.getRoles()==null? List.of() : user.getRoles().stream().map(Role::getName).toList();
        return Jwts.builder()
                .id(jwtId)
                .subject(user.getId().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTtlSeconds)))
                .claim("typ","refresh")
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();


    }

    //Parse The Token
    public Jws<Claims> parseToken(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }
    //Is Token AccessToken?
    public boolean isAccessToken(String token) {
        Claims payload = parseToken(token).getPayload();
        return "access".equals(payload.get("typ"));
    }
    //Is Token RefreshToken
    public boolean isRefreshToken(String token) {
        Claims payload = parseToken(token).getPayload();
        return "refresh".equals(payload.get("typ"));
    }

    //Get userId by token
    public UUID getUserId(String token) {
        Claims payload = parseToken(token).getPayload();
        return UUID.fromString( payload.getSubject());
    }

    public String getJwtId(String token) {
        return parseToken(token).getPayload().getId();
    }
    public List<String> getRoles(String token) {
        Claims payload = parseToken(token).getPayload();
        return (List<String>) payload.get("roles");
    }
    public String getEmail(String token) {
        Claims payload = parseToken(token).getPayload();
        return (String) payload.get("email");
    }
}
