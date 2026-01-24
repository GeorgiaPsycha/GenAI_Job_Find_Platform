package dev.genai.genaibe.services;

import dev.genai.genaibe.models.entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    // Βάλε αυτό στο application.properties: jwt.secret=MiaPoliMegaliMistikyFrasiGiaToToken123456
    @Value("${jwt.secret:MiaPoliMegaliMistikyFrasiGiaToToken1234567890}")
    private String secretKey;

    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getId().toString()) // Αποθηκεύουμε ΜΟΝΟ το ID
                .claim("email", user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // 24 ώρες
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUserId(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject(); // Επιστρέφει το ID που σώσαμε
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }
}