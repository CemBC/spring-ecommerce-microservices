package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.dto.RegisterRequest;
import com.ecommerce.auth_service.dto.UserResponse;
import com.ecommerce.auth_service.entity.User;
import com.ecommerce.auth_service.exception.ConflictException;
import com.ecommerce.auth_service.mapper.UserMapper;
import com.ecommerce.auth_service.repository.UserRepository;
import com.ecommerce.auth_service.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    }

    @Test
    void shouldRegisterUser() {
        RegisterRequest request =
                new RegisterRequest(
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

        UserResponse response =
                authService.register(request);

        assertEquals(1L, response.id());
        assertEquals("Test User", response.fullName());
        assertEquals("test@example.com", response.email());
        assertTrue(response.active());

        verify(passwordEncoder).encode(request.password());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldRejectDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("test@example.com"))
                .thenReturn(true);

        RegisterRequest request =
                new RegisterRequest(
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
}
