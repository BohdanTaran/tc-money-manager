package org.tc.mtracker.support.factory;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tc.mtracker.user.User;

import java.time.Duration;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTestTokenFactory {

    private final String secretKey;

    public JwtTestTokenFactory(@Value("${security.jwt.secret-key}") String secretKey) {
        this.secretKey = secretKey;
    }

    public String accessToken(String email) {
        return token(email, "access_token", Duration.ofHours(1));
    }

    public String accessToken(User user) {
        return accessToken(user.getEmail());
    }

    public String bearerAccessToken(String email) {
        return "Bearer " + accessToken(email);
    }

    public String bearerAccessToken(User user) {
        return bearerAccessToken(user.getEmail());
    }

    public String token(String email, String purpose, Duration ttl) {
        return Jwts.builder()
                .setClaims(Map.of("purpose", purpose))
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ttl.toMillis()))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey)), SignatureAlgorithm.HS256)
                .compact();
    }
}
