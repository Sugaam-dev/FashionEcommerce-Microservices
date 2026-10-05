package com.pmrgsolution.authservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;

public record RefreshTokenRequest(
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String refreshToken
) {
}
