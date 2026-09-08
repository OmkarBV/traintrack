package com.traintrack.coreapi.auth;

import com.traintrack.coreapi.auth.dto.TokenResponse;
import com.traintrack.coreapi.common.exception.InvalidCredentialsException;
import com.traintrack.coreapi.common.exception.InvalidTokenException;
import com.traintrack.coreapi.domain.RefreshToken;
import com.traintrack.coreapi.domain.User;
import com.traintrack.coreapi.domain.UserStatus;
import com.traintrack.coreapi.rbac.PermissionRepository;
import com.traintrack.coreapi.security.JwtProperties;
import com.traintrack.coreapi.security.JwtService;
import com.traintrack.coreapi.user.UserRepository;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PermissionRepository permissionRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.permissionRepository = permissionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public TokenResponse login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
        if (user.getStatus() != UserStatus.ACTIVE || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(String refreshJwt) {
        JwtService.RefreshTokenClaims claims = jwtService.parseRefreshToken(refreshJwt);
        RefreshToken record = refreshTokenRepository
                .findById(claims.tokenId())
                .filter(RefreshToken::isUsable)
                .orElseThrow(() -> new InvalidTokenException("Refresh token is invalid, expired, or revoked"));
        // Rotation: this refresh token is single-use, so a stolen-and-replayed
        // token stops working the moment the legitimate client refreshes first.
        record.revoke();
        User user = userRepository
                .findById(claims.userId())
                .orElseThrow(() -> new InvalidTokenException("Refresh token is invalid, expired, or revoked"));
        return issueTokens(user);
    }

    @Transactional
    public void logout(String refreshJwt) {
        JwtService.RefreshTokenClaims claims = jwtService.parseRefreshToken(refreshJwt);
        refreshTokenRepository.findById(claims.tokenId()).ifPresent(RefreshToken::revoke);
    }

    private TokenResponse issueTokens(User user) {
        Set<String> permissions = permissionRepository.findCodesByUserId(user.getId());
        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getOrganisation().getId(), user.getEmail(), permissions);

        UUID refreshTokenId = UUID.randomUUID();
        Instant refreshExpiresAt = Instant.now().plus(jwtProperties.refreshTokenTtl());
        refreshTokenRepository.save(new RefreshToken(refreshTokenId, user, refreshExpiresAt));
        String refreshToken = jwtService.generateRefreshToken(user.getId(), refreshTokenId, refreshExpiresAt);

        return new TokenResponse(
                accessToken, refreshToken, "Bearer", jwtProperties.accessTokenTtl().toSeconds());
    }
}
