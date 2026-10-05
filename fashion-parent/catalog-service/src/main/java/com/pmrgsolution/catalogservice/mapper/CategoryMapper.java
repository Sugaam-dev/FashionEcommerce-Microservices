package com.pmrgsolution.catalogservice.mapper;

import com.pmrgsolution.catalogservice.dto.request.CategoryRequest;
import com.pmrgsolution.catalogservice.dto.response.CategoryResponse;
import com.pmrgsolution.catalogservice.entity.Category;

import java.util.Collections;
import java.util.List;

public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static Category toEntity(CategoryRequest request, Category parent) {
        if (request == null) {
            return null;
        }
        return Category.builder()
                .name(request.name().trim())
                .slug(request.slug() != null ? request.slug().trim() : null)
                .description(request.description())
                .parent(parent)
                .iconUrl(request.iconUrl())
                .bannerUrl(request.bannerUrl())
                .displayOrder(request.displayOrder())
                .isActive(request.isActive())
                .build();
    }

    public static void updateEntity(Category category, CategoryRequest request, Category parent) {
        if (category == null || request == null) {
            return;
        }
        category.setName(request.name().trim());
        if (request.slug() != null && !request.slug().isBlank()) {
            category.setSlug(request.slug().trim());
        }
        category.setDescription(request.description());
        category.setParent(parent);
        category.setIconUrl(request.iconUrl());
        category.setBannerUrl(request.bannerUrl());
        category.setDisplayOrder(request.displayOrder());
        category.setActive(request.isActive());
    }

    public static CategoryResponse toResponse(Category category) {
        if (category == null) {
            return null;
        }
        List<CategoryResponse> subCategories = category.getSubCategories() != null
                ? category.getSubCategories().stream()
                .filter(Category::isActive)
                .map(CategoryMapper::toResponse)
                .toList()
                : Collections.emptyList();

        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getParent() != null ? category.getParent().getId() : null,
                category.getIconUrl(),
                category.getBannerUrl(),
                category.getDisplayOrder(),
                category.isActive(),
                subCategories
        );
    }
}
