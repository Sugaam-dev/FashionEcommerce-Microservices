package com.pmrgsolution.catalogservice.dto.response;

public record ImageUploadResponse(
        String originalUrl,
        String thumbnailUrl,
        String mimeType,
        long sizeBytes
) {
}
