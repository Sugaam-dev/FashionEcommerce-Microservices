package com.pmrgsolution.catalogservice.mapper;

import com.pmrgsolution.catalogservice.dto.request.VariantCreateRequest;
import com.pmrgsolution.catalogservice.dto.response.VariantResponse;
import com.pmrgsolution.catalogservice.entity.Product;
import com.pmrgsolution.catalogservice.entity.ProductVariant;

public final class ProductVariantMapper {

    private ProductVariantMapper() {
    }

    public static ProductVariant toEntity(VariantCreateRequest request, Product product) {
        if (request == null) {
            return null;
        }
        return ProductVariant.builder()
                .product(product)
                .sku(request.sku().trim())
                .size(request.size() != null ? request.size().trim() : null)
                .color(request.color() != null ? request.color().trim() : null)
                .colorHex(request.colorHex() != null ? request.colorHex().trim() : null)
                .price(request.price())
                .compareAtPrice(request.compareAtPrice())
                .variantImageUrl(request.variantImageUrl())
                .isActive(request.isActive())
                .build();
    }

    public static VariantResponse toResponse(ProductVariant variant) {
        if (variant == null) {
            return null;
        }
        return new VariantResponse(
                variant.getId(),
                variant.getSku(),
                variant.getSize(),
                variant.getColor(),
                variant.getColorHex(),
                variant.getPrice(),
                variant.getCompareAtPrice(),
                variant.getVariantImageUrl(),
                variant.isActive()
        );
    }
}
