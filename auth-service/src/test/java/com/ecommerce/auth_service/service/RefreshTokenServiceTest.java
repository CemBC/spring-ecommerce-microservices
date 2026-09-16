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
        User user = User.builder()
                .id(1L)
                .active(true)
                .build();

        RefreshTokenService.IssuedRefreshToken result =
                service.issue(user);

        assertNotNull(result.token());
        assertFalse(result.token().isBlank());
        assertEquals(604800, result.expiresIn());

        verify(repository).save(any(RefreshToken.class));
    }

    @Test
    void shouldRejectRevokedToken() {
        RefreshToken token = RefreshToken.builder()
                .revoked(true)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .user(User.builder().id(1L).active(true).build())
                .build();

        when(repository.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(token));

        assertThrows(
                UnauthorizedException.class,
                () -> service.rotate("raw-token")
        );
    }

    @Test
    void shouldRejectExpiredToken() {
        RefreshToken token = RefreshToken.builder()
                .revoked(false)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .user(User.builder().id(1L).active(true).build())
                .build();

        when(repository.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(token));

        assertThrows(
                UnauthorizedException.class,
                () -> service.rotate("raw-token")
        );

        assertTrue(token.isRevoked());
    }
}
