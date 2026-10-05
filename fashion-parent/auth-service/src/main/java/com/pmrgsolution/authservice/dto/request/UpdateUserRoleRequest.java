package com.pmrgsolution.authservice.dto.request;

import com.pmrgsolution.authservice.enums.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull(message = "Role is required")
        Role role
) {
}
