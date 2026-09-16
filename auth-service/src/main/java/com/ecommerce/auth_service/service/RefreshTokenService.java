package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.entity.RefreshToken;
import com.ecommerce.auth_service.entity.User;
import com.ecommerce.auth_service.exception.UnauthorizedException;
import com.ecommerce.auth_service.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long expirationMs;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${security.refresh-token.expiration-ms}") long expirationMs
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.expirationMs = expirationMs;
    }

    @Transactional
    public IssuedRefreshToken issue(User user) {
        String rawToken = generateToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hash(rawToken))
                .expiresAt(
                        LocalDateTime.now()
                                .plus(Duration.ofMillis(expirationMs))
                )
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        return new IssuedRefreshToken(
                rawToken,
                expirationMs / 1000
        );
    }

    @Transactional
    public RotatedRefreshToken rotate(String rawToken) {
        RefreshToken current = refreshTokenRepository
                .findByTokenHashForUpdate(hash(rawToken))
                .orElseThrow(() ->
                        new UnauthorizedException("Invalid refresh token")
                );

        if (current.isRevoked()) {
            throw new UnauthorizedException(
                    "Refresh token has been revoked"
            );
        }

        if (current.getExpiresAt().isBefore(LocalDateTime.now())) {
            current.setRevoked(true);

            throw new UnauthorizedException(
                    "Refresh token has expired"
            );
        }

        User user = current.getUser();

        if (!user.isActive()) {
            current.setRevoked(true);

            throw new UnauthorizedException(
                    "Account is disabled"
            );
        }

        current.setRevoked(true);

        IssuedRefreshToken next = issue(user);

        return new RotatedRefreshToken(
                user,
                next.token(),
                next.expiresIn()
        );
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository
                .findByTokenHashForUpdate(hash(rawToken))
                .ifPresent(token ->
                        token.setRevoked(true)
                );
    }

    @Transactional
    public void revokeAll(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteByExpiresAtBefore(
                LocalDateTime.now()
        );
    }

    private String generateToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hashed = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashed);

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "SHA-256 is not available",
                    ex
            );
        }
    }

    public record IssuedRefreshToken(
            String token,
            long expiresIn
    ) {
    }

    public record RotatedRefreshToken(
            User user,
            String token,
            long expiresIn
    ) {
    }
}
