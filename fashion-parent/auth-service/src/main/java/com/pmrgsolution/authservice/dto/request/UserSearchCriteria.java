package com.pmrgsolution.authservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.authservice.enums.AccountStatus;
import com.pmrgsolution.authservice.enums.AuthProvider;
import com.pmrgsolution.authservice.enums.Role;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;

public record UserSearchCriteria(
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String query,

        Role role,
        AccountStatus status,
        AuthProvider authProvider,
        Boolean emailVerified
) {
}
