package com.ecommerce.auth_service.controller;

import com.ecommerce.auth_service.dto.UpdateUserActiveRequest;
import com.ecommerce.auth_service.dto.UpdateUserRoleRequest;
import com.ecommerce.auth_service.dto.UserResponse;
import com.ecommerce.auth_service.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public Page<UserResponse> getAll(
            @PageableDefault(size = 10, sort = "id")
            Pageable pageable
    ) {
        return adminUserService.getAll(pageable);
    }

    @GetMapping("/{id}")
    public UserResponse getById(
            @PathVariable Long id
    ) {
        return adminUserService.getById(id);
    }

    @PatchMapping("/{id}/active")
    public UserResponse setActive(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserActiveRequest request
    ) {
        return adminUserService.setActive(
                id,
                request.active()
        );
    }

    @PatchMapping("/{id}/role")
    public UserResponse setRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        return adminUserService.setRole(
                id,
                request.role()
        );
    }
}
