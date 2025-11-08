package com.dreammapper.service.impl;

import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    public String generateToken(String subject, Map<String, Object> claims) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + jwtExpirationMs);
        return Jwts.builder()
                .setSubject(subject)
                .addClaims(claims)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractSubject(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public Long extractUserId(String token) {
        try {
            var claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            Object uid = claims.get("uid");
            if (uid instanceof Number) {
                return ((Number) uid).longValue();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private SecretKey getSigningKey() {
        // Ensure key is at least 256 bits (32 bytes)
        byte[] keyBytes;
        try {
            // Try base64 first
            keyBytes = java.util.Base64.getDecoder().decode(jwtSecret);
        } catch (IllegalArgumentException ex) {
            // Fallback to raw bytes
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }
        
        // Ensure minimum 32 bytes for HS256
        if (keyBytes.length < 32) {
            // Pad with zeros or repeat the key
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
            for (int i = keyBytes.length; i < 32; i++) {
                paddedKey[i] = keyBytes[i % keyBytes.length];
            }
            keyBytes = paddedKey;
        }
        
        return Keys.hmacShaKeyFor(keyBytes);
    }
}