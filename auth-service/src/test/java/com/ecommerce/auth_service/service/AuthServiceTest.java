package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.dto.*;
import com.ecommerce.auth_service.entity.Role;
import com.ecommerce.auth_service.entity.User;
import com.ecommerce.auth_service.exception.ConflictException;
import com.ecommerce.auth_service.exception.UnauthorizedException;
import com.ecommerce.auth_service.mapper.UserMapper;
import com.ecommerce.auth_service.repository.UserRepository;
import com.ecommerce.auth_service.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;
    private JwtService jwtService;
    private RefreshTokenService refreshTokenService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        authenticationManager = mock(AuthenticationManager.class);
        jwtService = mock(JwtService.class);
        refreshTokenService = mock(RefreshTokenService.class);

        authService = new AuthService(
                userRepository,
                passwordEncoder,
                authenticationManager,
                jwtService,
                new UserMapper(),
                refreshTokenService
        );

        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRegisterUser() {
        RegisterRequest request = new RegisterRequest(
                "Test User",
                "TEST@example.com",
                "StrongPassword123!"
        );

        when(userRepository.existsByEmailIgnoreCase("test@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode(request.password()))
                .thenReturn("hashed-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(1L);
                    return user;
                });

        UserResponse response = authService.register(request);

        assertEquals(1L, response.id());
        assertEquals("Test User", response.fullName());
        assertEquals("test@example.com", response.email());
        assertEquals(Role.USER, response.role());
        assertTrue(response.active());

        verify(passwordEncoder).encode(request.password());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldRejectDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("test@example.com"))
                .thenReturn(true);

        RegisterRequest request = new RegisterRequest(
                "Test User",
                "test@example.com",
                "StrongPassword123!"
        );

        assertThrows(
                ConflictException.class,
                () -> authService.register(request)
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldLoginAndReturnAccessAndRefreshTokens() {
        User user = activeUser();

        when(userRepository.findByEmailIgnoreCase("test@example.com"))
                .thenReturn(Optional.of(user));

        when(jwtService.generateToken(user))
                .thenReturn("access-token");

        when(jwtService.getExpirationSeconds())
                .thenReturn(900L);

        when(refreshTokenService.issue(user))
                .thenReturn(
                        new RefreshTokenService.IssuedRefreshToken(
                                "refresh-token",
                                604800L
                        )
                );

        AuthResponse response = authService.login(
                new LoginRequest(
                        "TEST@example.com",
                        "StrongPassword123!"
                )
        );

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900L, response.expiresIn());
        assertEquals(604800L, response.refreshExpiresIn());

        verify(authenticationManager).authenticate(any());
        verify(refreshTokenService).issue(user);
    }

    @Test
    void shouldRefreshTokens() {
        User user = activeUser();

        when(refreshTokenService.rotate("old-refresh"))
                .thenReturn(
                        new RefreshTokenService.RotatedRefreshToken(
                                user,
                                "new-refresh",
                                604800L
                        )
                );

        when(jwtService.generateToken(user))
                .thenReturn("new-access");

        when(jwtService.getExpirationSeconds())
                .thenReturn(900L);

        AuthResponse response = authService.refresh(
                new RefreshTokenRequest("old-refresh")
        );

        assertEquals("new-access", response.accessToken());
        assertEquals("new-refresh", response.refreshToken());

        verify(refreshTokenService).rotate("old-refresh");
    }

    @Test
    void shouldLogoutSingleRefreshToken() {
        authService.logout(
                new RefreshTokenRequest("refresh-token")
        );

        verify(refreshTokenService)
                .revoke("refresh-token");
    }

    @Test
    void shouldLogoutAllSessions() {
        User user = activeUser();

        mockAuthenticatedUser(user);
        when(userRepository.findByEmailIgnoreCase(user.getEmail()))
                .thenReturn(Optional.of(user));

        authService.logoutAll();

        verify(refreshTokenService)
                .revokeAll(user.getId());
    }

    @Test
    void shouldChangePasswordAndRevokeAllRefreshTokens() {
        User user = activeUser();

        mockAuthenticatedUser(user);

        when(userRepository.findByEmailIgnoreCase(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "CurrentPassword123!",
                user.getPasswordHash()
        )).thenReturn(true);

        when(passwordEncoder.matches(
                "NewPassword123!",
                user.getPasswordHash()
        )).thenReturn(false);

        when(passwordEncoder.encode("NewPassword123!"))
                .thenReturn("new-hash");

        authService.changePassword(
                new ChangePasswordRequest(
                        "CurrentPassword123!",
                        "NewPassword123!"
                )
        );

        assertEquals("new-hash", user.getPasswordHash());

        verify(refreshTokenService)
                .revokeAll(user.getId());
    }

    @Test
    void shouldRejectWrongCurrentPassword() {
        User user = activeUser();

        mockAuthenticatedUser(user);

        when(userRepository.findByEmailIgnoreCase(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "WrongPassword",
                user.getPasswordHash()
        )).thenReturn(false);

        assertThrows(
                UnauthorizedException.class,
                () -> authService.changePassword(
                        new ChangePasswordRequest(
                                "WrongPassword",
                                "NewPassword123!"
                        )
                )
        );

        verify(refreshTokenService, never())
                .revokeAll(anyLong());
    }

    @Test
    void shouldRejectSamePasswordAsNewPassword() {
        User user = activeUser();

        mockAuthenticatedUser(user);

        when(userRepository.findByEmailIgnoreCase(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "CurrentPassword123!",
                user.getPasswordHash()
        )).thenReturn(true);

        when(passwordEncoder.matches(
                "CurrentPassword123!",
                user.getPasswordHash()
        )).thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> authService.changePassword(
                        new ChangePasswordRequest(
                                "CurrentPassword123!",
                                "CurrentPassword123!"
                        )
                )
        );

        verify(refreshTokenService, never())
                .revokeAll(anyLong());
    }

    private User activeUser() {
        return User.builder()
                .id(1L)
                .fullName("Test User")
                .email("test@example.com")
                .passwordHash("old-hash")
                .role(Role.USER)
                .active(true)
                .build();
    }

    private void mockAuthenticatedUser(User user) {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(authentication.getName())
                .thenReturn(user.getEmail());

        when(securityContext.getAuthentication())
                .thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }
}
