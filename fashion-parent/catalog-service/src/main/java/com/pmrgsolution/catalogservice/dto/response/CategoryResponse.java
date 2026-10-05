package com.pmrgsolution.catalogservice.dto.response;

import java.util.List;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String slug,
        String description,
        UUID parentId,
        String iconUrl,
        String bannerUrl,
        int displayOrder,
        boolean isActive,
        List<CategoryResponse> subCategories
) {
}
