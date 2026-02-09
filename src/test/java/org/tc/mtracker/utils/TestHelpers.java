package org.tc.mtracker.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

@Component
public class TestHelpers {

    @Autowired
    private Environment env;

    /**
     * Helper to generate JWTs that match the Test configuration
     */
    public String generateTestToken(String email, String purpose, long expirationOffsetMs) {
        String secretKey = env.getProperty("security.jwt.secret-key");

        byte[] keyBytes = Decoders.BASE64.decode(secretKey);

        return Jwts.builder()
                .setClaims(Map.of("purpose", purpose))
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationOffsetMs))
                .signWith(Keys.hmacShaKeyFor(keyBytes), SignatureAlgorithm.HS256)
                .compact();
    }
}
