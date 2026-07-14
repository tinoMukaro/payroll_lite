package com.tino.payroll.lite.service;



import com.tino.payroll.lite.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public String generateToken(User user){
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .claim("userId", user.getId())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }
    public boolean isTokenValid(String token, User user) {
        String email = extractEmail(token);

        return email.equals(user.getEmail())
                && !isTokenExpired(token);
    }
    private boolean isTokenExpired(String token) {
        Date expiration = extractAllClaims(token).getExpiration();
        return expiration.before(new Date());
    }
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(
                jwtSecret.getBytes(StandardCharsets.UTF_8)
        );
    }
}
