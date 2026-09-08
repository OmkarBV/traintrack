package com.traintrack.coreapi.security;

import com.traintrack.coreapi.common.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * Issues and validates JWTs. Access tokens carry the caller's permission
 * codes as a claim, signed by the server — so an endpoint's
 * {@code @PreAuthorize} check reads authorities straight off the verified
 * token instead of querying role_permissions on every request. The
 * trade-off: a permission revoked mid-session stays valid until that access
 * token expires. Keeping {@code accessTokenTtl} short (default 15 minutes,
 * see application.yml) bounds that staleness window; anything shorter starts
 * trading it for more login/refresh traffic.
 */
@Component
public class JwtService {

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ORG_ID = "orgId";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_PERMISSIONS = "permissions";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, UUID orgId, String email, Set<String> permissions) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .claim(CLAIM_ORG_ID, orgId.toString())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_PERMISSIONS, permissions.stream().toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTokenTtl())))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(UUID userId, UUID tokenId, Instant expiresAt) {
        return Jwts.builder()
                .subject(userId.toString())
                .id(tokenId.toString())
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
    }

    public AuthenticatedUser parseAccessToken(String token) {
        Claims claims = parse(token);
        requireType(claims, TYPE_ACCESS);
        @SuppressWarnings("unchecked")
        List<String> permissions = claims.get(CLAIM_PERMISSIONS, List.class);
        return new AuthenticatedUser(
                UUID.fromString(claims.getSubject()),
                UUID.fromString(claims.get(CLAIM_ORG_ID, String.class)),
                claims.get(CLAIM_EMAIL, String.class),
                Set.copyOf(permissions));
    }

    public RefreshTokenClaims parseRefreshToken(String token) {
        Claims claims = parse(token);
        requireType(claims, TYPE_REFRESH);
        return new RefreshTokenClaims(UUID.fromString(claims.getSubject()), UUID.fromString(claims.getId()));
    }

    private Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Invalid or expired token");
        }
    }

    private void requireType(Claims claims, String expectedType) {
        if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new InvalidTokenException("Unexpected token type");
        }
    }

    public record RefreshTokenClaims(UUID userId, UUID tokenId) {
    }
}
