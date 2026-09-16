package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.dto.UserResponse;
import com.ecommerce.auth_service.entity.Role;
import com.ecommerce.auth_service.entity.User;
import com.ecommerce.auth_service.exception.ResourceNotFoundException;
import com.ecommerce.auth_service.mapper.UserMapper;
import com.ecommerce.auth_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminUserServiceTest {

    private UserRepository userRepository;
    private RefreshTokenService refreshTokenService;
    private AdminUserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        refreshTokenService = mock(RefreshTokenService.class);

        service = new AdminUserService(
                userRepository,
                new UserMapper(),
                refreshTokenService
        );
    }

    @Test
    void shouldDisableUserAndRevokeAllRefreshTokens() {
        User user = user();

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        UserResponse response =
                service.setActive(1L, false);

        assertFalse(response.active());
        assertFalse(user.isActive());

        verify(refreshTokenService)
                .revokeAll(1L);
    }

    @Test
    void shouldEnableUserWithoutRevokingRefreshTokens() {
        User user = user();
        user.setActive(false);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        UserResponse response =
                service.setActive(1L, true);

        assertTrue(response.active());

        verify(refreshTokenService, never())
                .revokeAll(anyLong());
    }

    @Test
    void shouldChangeRoleAndRevokeRefreshTokens() {
        User user = user();

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        UserResponse response =
                service.setRole(1L, Role.ADMIN);

        assertEquals(Role.ADMIN, response.role());
        assertEquals(Role.ADMIN, user.getRole());

        verify(refreshTokenService)
                .revokeAll(1L);
    }

    @Test
    void shouldThrowWhenAdminRequestsMissingUser() {
        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.getById(999L)
        );
    }

    private User user() {
        return User.builder()
                .id(1L)
                .fullName("Test User")
                .email("test@example.com")
                .passwordHash("hash")
                .role(Role.USER)
                .active(true)
                .build();
    }
}
