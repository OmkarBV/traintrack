package com.traintrack.coreapi.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<AuthenticatedUser> get() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return Optional.empty();
        }
        return Optional.of(user);
    }

    public static Optional<UUID> currentOrgId() {
        return get().map(AuthenticatedUser::orgId);
    }

    public static UUID requireUserId() {
        return get().map(AuthenticatedUser::userId)
                .orElseThrow(() -> new IllegalStateException("No authenticated user in context"));
    }

    public static UUID requireOrgId() {
        return currentOrgId().orElseThrow(() -> new IllegalStateException("No authenticated user in context"));
    }
}
