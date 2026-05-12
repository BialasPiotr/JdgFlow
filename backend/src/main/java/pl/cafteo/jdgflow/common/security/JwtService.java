package pl.cafteo.jdgflow.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.cafteo.jdgflow.common.config.JwtProperties;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class JwtService implements TokenIssuer, TokenParser {

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        byte[] secretBytes = Base64.getDecoder().decode(padBase64(properties.secret()));
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT secret must decode to at least 32 bytes (got " + secretBytes.length + ")");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
    }

    @Override
    public String issueToken(UUID userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(now.plus(properties.ttl())))
                .signWith(signingKey)
                .compact();
    }

    @Override
    public Optional<TokenClaims> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            UUID userId = UUID.fromString(claims.getSubject());
            String email = claims.get("email", String.class);
            return Optional.of(new TokenClaims(userId, email));
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private static String padBase64(String value) {
        int rem = value.length() % 4;
        return rem == 0 ? value : value + "=".repeat(4 - rem);
    }
}
