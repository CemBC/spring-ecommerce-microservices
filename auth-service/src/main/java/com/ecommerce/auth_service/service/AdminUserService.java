package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.dto.UserResponse;
import com.ecommerce.auth_service.entity.Role;
import com.ecommerce.auth_service.entity.User;
import com.ecommerce.auth_service.exception.ResourceNotFoundException;
import com.ecommerce.auth_service.mapper.UserMapper;
import com.ecommerce.auth_service.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RefreshTokenService refreshTokenService;

    public AdminUserService(
            UserRepository userRepository,
            UserMapper userMapper,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAll(Pageable pageable) {
        return userRepository
                .findAll(pageable)
                .map(userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return userMapper.toResponse(findUser(id));
    }

    @Transactional
    public UserResponse setActive(Long id, boolean active) {
        User user = findUser(id);

        user.setActive(active);

        if (!active) {
            refreshTokenService.revokeAll(id);
        }

        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse setRole(Long id, Role role) {
        User user = findUser(id);

        user.setRole(role);

        // Force the user to authenticate again so future access
        // tokens reflect the new role.
        refreshTokenService.revokeAll(id);

        return userMapper.toResponse(user);
    }

    private User findUser(Long id) {
        return userRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + id
                        )
                );
    }
}
