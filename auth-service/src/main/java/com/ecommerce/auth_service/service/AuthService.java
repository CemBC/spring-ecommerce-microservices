package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.dto.*;
import com.ecommerce.auth_service.entity.Role;
import com.ecommerce.auth_service.entity.User;
import com.ecommerce.auth_service.exception.ConflictException;
import com.ecommerce.auth_service.exception.ResourceNotFoundException;
import com.ecommerce.auth_service.exception.UnauthorizedException;
import com.ecommerce.auth_service.mapper.UserMapper;
import com.ecommerce.auth_service.repository.UserRepository;
import com.ecommerce.auth_service.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserMapper userMapper,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String normalizedEmail =
                request.email().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException(
                    "Email is already registered"
            );
        }

        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(normalizedEmail)
                .passwordHash(
                        passwordEncoder.encode(request.password())
                )
                .role(Role.USER)
                .active(true)
                .build();

        return userMapper.toResponse(
                userRepository.save(user)
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail =
                request.email().trim().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        normalizedEmail,
                        request.password()
                )
        );

        User user = findByEmail(normalizedEmail);

        String accessToken =
                jwtService.generateToken(user);

        RefreshTokenService.IssuedRefreshToken refreshToken =
                refreshTokenService.issue(user);

        return new AuthResponse(
                accessToken,
                refreshToken.token(),
                "Bearer",
                jwtService.getExpirationSeconds(),
                refreshToken.expiresIn()
        );
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshTokenService.RotatedRefreshToken refreshToken =
                refreshTokenService.rotate(
                        request.refreshToken()
                );

        String accessToken =
                jwtService.generateToken(
                        refreshToken.user()
                );

        return new AuthResponse(
                accessToken,
                refreshToken.token(),
                "Bearer",
                jwtService.getExpirationSeconds(),
                refreshToken.expiresIn()
        );
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revoke(
                request.refreshToken()
        );
    }

    @Transactional
    public void logoutAll() {
        User user = getCurrentUserEntity();

        refreshTokenService.revokeAll(
                user.getId()
        );
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUserEntity();

        if (!passwordEncoder.matches(
                request.currentPassword(),
                user.getPasswordHash()
        )) {
            throw new UnauthorizedException(
                    "Current password is incorrect"
            );
        }

        if (passwordEncoder.matches(
                request.newPassword(),
                user.getPasswordHash()
        )) {
            throw new ConflictException(
                    "New password must be different from the current password"
            );
        }

        user.setPasswordHash(
                passwordEncoder.encode(
                        request.newPassword()
                )
        );

        refreshTokenService.revokeAll(
                user.getId()
        );
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        return userMapper.toResponse(
                getCurrentUserEntity()
        );
    }

    private User getCurrentUserEntity() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        return findByEmail(authentication.getName());
    }

    private User findByEmail(String email) {
        return userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );
    }
}
