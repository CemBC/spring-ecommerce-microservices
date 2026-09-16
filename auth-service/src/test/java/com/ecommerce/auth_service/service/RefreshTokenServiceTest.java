package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.entity.RefreshToken;
import com.ecommerce.auth_service.entity.User;
import com.ecommerce.auth_service.exception.UnauthorizedException;
import com.ecommerce.auth_service.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {

    private RefreshTokenRepository repository;
    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        repository = mock(RefreshTokenRepository.class);

        service = new RefreshTokenService(
                repository,
                604800000
        );
    }

    @Test
    void shouldIssueRefreshToken() {
        User user = activeUser();

        RefreshTokenService.IssuedRefreshToken result =
                service.issue(user);

        assertNotNull(result.token());
        assertFalse(result.token().isBlank());
        assertEquals(604800L, result.expiresIn());

        verify(repository)
                .save(any(RefreshToken.class));
    }

    @Test
    void shouldRotateRefreshToken() {
        User user = activeUser();

        RefreshToken current = RefreshToken.builder()
                .id(1L)
                .user(user)
                .tokenHash("old-hash")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(false)
                .build();

        when(repository.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(current));

        RefreshTokenService.RotatedRefreshToken result =
                service.rotate("old-raw-token");

        assertTrue(current.isRevoked());
        assertSame(user, result.user());
        assertNotNull(result.token());
        assertFalse(result.token().isBlank());
        assertEquals(604800L, result.expiresIn());

        verify(repository).save(any(RefreshToken.class));
    }

    @Test
    void shouldRejectReusingRotatedRefreshToken() {
        User user = activeUser();

        RefreshToken current = RefreshToken.builder()
                .id(1L)
                .user(user)
                .tokenHash("old-hash")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(false)
                .build();

        when(repository.findByTokenHashForUpdate(anyString()))
                .thenReturn(
                        Optional.of(current),
                        Optional.of(current)
                );

        service.rotate("old-raw-token");

        assertTrue(current.isRevoked());

        assertThrows(
                UnauthorizedException.class,
                () -> service.rotate("old-raw-token")
        );
    }

    @Test
    void shouldRejectAlreadyRevokedToken() {
        RefreshToken token = RefreshToken.builder()
                .revoked(true)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .user(activeUser())
                .build();

        when(repository.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(token));

        assertThrows(
                UnauthorizedException.class,
                () -> service.rotate("raw-token")
        );
    }

    @Test
    void shouldRejectExpiredTokenAndMarkItRevoked() {
        RefreshToken token = RefreshToken.builder()
                .revoked(false)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .user(activeUser())
                .build();

        when(repository.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(token));

        assertThrows(
                UnauthorizedException.class,
                () -> service.rotate("raw-token")
        );

        assertTrue(token.isRevoked());
    }

    @Test
    void shouldRejectRefreshForDisabledUser() {
        User disabledUser = activeUser();
        disabledUser.setActive(false);

        RefreshToken token = RefreshToken.builder()
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .user(disabledUser)
                .build();

        when(repository.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(token));

        assertThrows(
                UnauthorizedException.class,
                () -> service.rotate("raw-token")
        );

        assertTrue(token.isRevoked());
    }

    @Test
    void shouldRevokeSingleRefreshToken() {
        RefreshToken token = RefreshToken.builder()
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .user(activeUser())
                .build();

        when(repository.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(token));

        service.revoke("raw-token");

        assertTrue(token.isRevoked());
    }

    @Test
    void shouldRevokeAllUserRefreshTokens() {
        service.revokeAll(15L);

        verify(repository)
                .revokeAllByUserId(15L);
    }

    private User activeUser() {
        return User.builder()
                .id(1L)
                .email("test@example.com")
                .active(true)
                .build();
    }
}
