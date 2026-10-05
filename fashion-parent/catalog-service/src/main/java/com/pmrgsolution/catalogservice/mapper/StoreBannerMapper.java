package com.pmrgsolution.catalogservice.mapper;

import com.pmrgsolution.catalogservice.dto.request.StoreBannerRequest;
import com.pmrgsolution.catalogservice.dto.response.StoreBannerResponse;
import com.pmrgsolution.catalogservice.entity.StoreBanner;

public final class StoreBannerMapper {

    private StoreBannerMapper() {
    }

    public static StoreBanner toEntity(StoreBannerRequest request) {
        if (request == null) {
            return null;
        }
        return StoreBanner.builder()
                .title(request.title().trim())
                .subtitle(request.subtitle() != null ? request.subtitle().trim() : null)
                .imageUrl(request.imageUrl().trim())
                .targetUrl(request.targetUrl() != null ? request.targetUrl().trim() : null)
                .position(request.position())
                .displayOrder(request.displayOrder())
                .isActive(request.isActive())
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .build();
    }

    public static void updateEntity(StoreBanner banner, StoreBannerRequest request) {
        if (banner == null || request == null) {
            return;
        }
        banner.setTitle(request.title().trim());
        banner.setSubtitle(request.subtitle() != null ? request.subtitle().trim() : null);
        banner.setImageUrl(request.imageUrl().trim());
        banner.setTargetUrl(request.targetUrl() != null ? request.targetUrl().trim() : null);
        banner.setPosition(request.position());
        banner.setDisplayOrder(request.displayOrder());
        banner.setActive(request.isActive());
        banner.setStartsAt(request.startsAt());
        banner.setEndsAt(request.endsAt());
    }

    public static StoreBannerResponse toResponse(StoreBanner banner) {
        if (banner == null) {
            return null;
        }
        return new StoreBannerResponse(
                banner.getId(),
                banner.getTitle(),
                banner.getSubtitle(),
                banner.getImageUrl(),
                banner.getTargetUrl(),
                banner.getPosition(),
                banner.getDisplayOrder(),
                banner.isActive(),
                banner.getStartsAt(),
                banner.getEndsAt()
        );
    }
}
