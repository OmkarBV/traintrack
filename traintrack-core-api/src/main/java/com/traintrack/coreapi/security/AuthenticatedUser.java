package com.traintrack.coreapi.security;

import java.util.Set;
import java.util.UUID;

/**
 * The security-context principal. Built entirely from JWT claims by
 * {@link JwtAuthenticationFilter} — never re-fetched from the database — so
 * that permission checks on the request path cost nothing beyond verifying
 * the token signature. See {@link JwtService} for the trade-off this implies.
 */
public record AuthenticatedUser(UUID userId, UUID orgId, String email, Set<String> permissions) {
}
