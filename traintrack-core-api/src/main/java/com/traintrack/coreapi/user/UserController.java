package com.traintrack.coreapi.user;

import com.traintrack.coreapi.security.CurrentUser;
import com.traintrack.coreapi.user.dto.UserResponse;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public UserResponse me() {
        return userMapper.toResponse(userService.getById(CurrentUser.requireUserId()));
    }

    /**
     * The org-isolation mechanism (see TenantAwareJpaTransactionManager) means
     * a caller with USER_MANAGE from Org A asking for an Org B user id gets a
     * plain 404 here, not a 403 — the row is invisible to their session, not
     * merely forbidden. See AuthOrgIsolationIntegrationTest.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public UserResponse getById(@PathVariable UUID id) {
        return userMapper.toResponse(userService.getById(id));
    }
}
