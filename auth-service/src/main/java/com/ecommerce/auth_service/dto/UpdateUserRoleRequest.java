package com.ecommerce.auth_service.dto;

import com.ecommerce.auth_service.entity.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull
        Role role
) {
}
