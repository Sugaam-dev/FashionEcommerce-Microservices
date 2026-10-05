package com.pmrgsolution.catalogservice.mapper;

import com.pmrgsolution.catalogservice.dto.request.ProductCreateRequest;
import com.pmrgsolution.catalogservice.dto.request.ProductUpdateRequest;
import com.pmrgsolution.catalogservice.dto.response.CategoryResponse;
import com.pmrgsolution.catalogservice.dto.response.ProductDetailResponse;
import com.pmrgsolution.catalogservice.dto.response.ProductSummaryResponse;
import com.pmrgsolution.catalogservice.dto.response.VariantResponse;
import com.pmrgsolution.catalogservice.entity.Category;
import com.pmrgsolution.catalogservice.entity.Product;
import com.pmrgsolution.catalogservice.entity.ProductVariant;
import com.pmrgsolution.catalogservice.enums.ProductStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ProductMapper {

    private ProductMapper() {
    }

    public static Product toEntity(ProductCreateRequest request, Category category, String slug) {
        if (request == null) {
            return null;
        }
        return Product.builder()
                .title(request.title().trim())
                .slug(slug)
                .skuPrefix(request.skuPrefix().trim().toUpperCase())
                .shortDescription(request.shortDescription())
                .description(request.description())
                .category(category)
                .brand(request.brand() != null ? request.brand().trim() : null)
                .status(ProductStatus.DRAFT)
                .basePrice(request.basePrice())
                .salePrice(request.salePrice())
                .tags(request.tags() != null ? new ArrayList<>(request.tags()) : new ArrayList<>())
                .imageUrls(new ArrayList<>())
                .averageRating(0.0)
                .reviewCount(0)
                .variants(new ArrayList<>())
                .build();
    }

    public static void updateEntity(Product product, ProductUpdateRequest request, Category category) {
        if (product == null || request == null) {
            return;
        }
        if (request.title() != null && !request.title().isBlank()) {
            product.setTitle(request.title().trim());
        }
        if (request.shortDescription() != null) {
            product.setShortDescription(request.shortDescription());
        }
        if (request.description() != null) {
            product.setDescription(request.description());
        }
        if (category != null) {
            product.setCategory(category);
        }
        if (request.brand() != null) {
            product.setBrand(request.brand().trim());
        }
        if (request.status() != null) {
            product.setStatus(request.status());
        }
        if (request.basePrice() != null) {
            product.setBasePrice(request.basePrice());
        }
        if (request.salePrice() != null) {
            product.setSalePrice(request.salePrice());
        }
        if (request.tags() != null) {
            product.setTags(new ArrayList<>(request.tags()));
        }
    }

    public static ProductSummaryResponse toSummaryResponse(Product product) {
        if (product == null) {
            return null;
        }
        String categoryName = product.getCategory() != null ? product.getCategory().getName() : null;
        return new ProductSummaryResponse(
                product.getId(),
                product.getTitle(),
                product.getSlug(),
                product.getBrand(),
                product.getBasePrice(),
                product.getSalePrice(),
                product.getPrimaryImageUrl(),
                product.getAverageRating(),
                product.getReviewCount(),
                product.getStatus(),
                categoryName
        );
    }

    public static ProductDetailResponse toDetailResponse(Product product, List<ProductVariant> variants) {
        if (product == null) {
            return null;
        }
        CategoryResponse categoryResponse = product.getCategory() != null
                ? CategoryMapper.toResponse(product.getCategory())
                : null;

        List<VariantResponse> variantResponses = variants != null
                ? variants.stream().map(ProductVariantMapper::toResponse).toList()
                : (product.getVariants() != null
                ? product.getVariants().stream().map(ProductVariantMapper::toResponse).toList()
                : Collections.emptyList());

        return new ProductDetailResponse(
                product.getId(),
                product.getTitle(),
                product.getSlug(),
                product.getSkuPrefix(),
                product.getShortDescription(),
                product.getDescription(),
                categoryResponse,
                product.getBrand(),
                product.getStatus(),
                product.getBasePrice(),
                product.getSalePrice(),
                product.getTags() != null ? new ArrayList<>(product.getTags()) : Collections.emptyList(),
                product.getImageUrls() != null ? new ArrayList<>(product.getImageUrls()) : Collections.emptyList(),
                product.getPrimaryImageUrl(),
                product.getAverageRating(),
                product.getReviewCount(),
                variantResponses,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
