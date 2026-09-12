package com.traintrack.coreapi.support;

import com.traintrack.coreapi.security.AuthenticatedUser;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Builds an {@link Authentication} carrying our custom {@link AuthenticatedUser}
 * principal, for {@code @WebMvcTest} slices to inject via Spring Security
 * Test's {@code authentication(...)} request post-processor — bypassing the
 * real JWT filter (which a slice test doesn't load) while still exercising
 * real {@code @PreAuthorize} method-security enforcement.
 */
public final class TestAuthentications {

    private TestAuthentications() {
    }

    public static Authentication withPermissions(Set<String> permissions) {
        return withPermissions(UUID.randomUUID(), UUID.randomUUID(), permissions);
    }

    public static Authentication withPermissions(UUID userId, UUID orgId, Set<String> permissions) {
        AuthenticatedUser principal = new AuthenticatedUser(userId, orgId, "user@acme.test", permissions);
        List<SimpleGrantedAuthority> authorities =
                permissions.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }
}
