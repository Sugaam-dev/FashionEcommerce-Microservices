package com.pmrgsolution.catalogservice.service;

import com.pmrgsolution.catalogservice.dto.request.ProductCreateRequest;
import com.pmrgsolution.catalogservice.dto.request.ProductFilterCriteria;
import com.pmrgsolution.catalogservice.dto.request.ProductUpdateRequest;
import com.pmrgsolution.catalogservice.dto.request.VariantCreateRequest;
import com.pmrgsolution.catalogservice.dto.response.ProductDetailResponse;
import com.pmrgsolution.catalogservice.dto.response.ProductSummaryResponse;
import com.pmrgsolution.catalogservice.dto.response.VariantResponse;
import com.pmrgsolution.catalogservice.enums.ProductStatus;
import com.pmrgsolution.shared.core.response.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ProductService {

    PageResponse<ProductSummaryResponse> getProducts(ProductFilterCriteria criteria, Pageable pageable);

    ProductDetailResponse getBySlug(String slug);

    ProductDetailResponse getById(UUID id);

    ProductDetailResponse createProduct(ProductCreateRequest request);

    ProductDetailResponse updateProduct(UUID id, ProductUpdateRequest request);

    VariantResponse addVariant(UUID productId, VariantCreateRequest request);

    VariantResponse updateVariant(UUID productId, UUID variantId, VariantCreateRequest request);

    void deleteVariant(UUID productId, UUID variantId);

    List<String> uploadProductImages(UUID productId, List<MultipartFile> files);

    void deleteProduct(UUID id);

    ProductDetailResponse updateProductStatus(UUID id, ProductStatus status);
}
