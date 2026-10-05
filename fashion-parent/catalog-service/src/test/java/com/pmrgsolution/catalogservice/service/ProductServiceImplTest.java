package com.pmrgsolution.catalogservice.service;

import com.pmrgsolution.catalogservice.dto.request.ProductCreateRequest;
import com.pmrgsolution.catalogservice.dto.request.ProductFilterCriteria;
import com.pmrgsolution.catalogservice.dto.request.VariantCreateRequest;
import com.pmrgsolution.catalogservice.dto.response.ProductDetailResponse;
import com.pmrgsolution.catalogservice.dto.response.ProductSummaryResponse;
import com.pmrgsolution.catalogservice.entity.Category;
import com.pmrgsolution.catalogservice.entity.Product;
import com.pmrgsolution.catalogservice.entity.ProductVariant;
import com.pmrgsolution.catalogservice.enums.ProductStatus;
import com.pmrgsolution.catalogservice.exception.CategoryNotFoundException;
import com.pmrgsolution.catalogservice.exception.DuplicateSkuException;
import com.pmrgsolution.catalogservice.exception.ProductNotFoundException;
import com.pmrgsolution.catalogservice.repository.CategoryRepository;
import com.pmrgsolution.catalogservice.repository.ProductRepository;
import com.pmrgsolution.catalogservice.repository.ProductVariantRepository;
import com.pmrgsolution.catalogservice.service.impl.ProductServiceImpl;
import com.pmrgsolution.shared.core.response.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private S3StorageService s3StorageService;

    @InjectMocks
    private ProductServiceImpl productService;

    private UUID productId;
    private UUID categoryId;
    private Category category;
    private Product product;
    private ProductCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        category = Category.builder()
                .id(categoryId)
                .name("Shirts")
                .slug("shirts")
                .build();

        product = Product.builder()
                .id(productId)
                .title("Slim Fit Oxford Shirt")
                .slug("slim-fit-oxford-shirt")
                .skuPrefix("OXF")
                .category(category)
                .brand("PMRG")
                .status(ProductStatus.PUBLISHED)
                .basePrice(new BigDecimal("1299.00"))
                .tags(new ArrayList<>(List.of("cotton", "formal")))
                .imageUrls(new ArrayList<>())
                .variants(new ArrayList<>())
                .build();

        VariantCreateRequest variantReq = new VariantCreateRequest(
                "OXF-SLIM-BLU-M",
                "M",
                "Blue",
                "#0000FF",
                new BigDecimal("1299.00"),
                null,
                "variant.jpg",
                true
        );

        createRequest = new ProductCreateRequest(
                "Slim Fit Oxford Shirt",
                "OXF",
                "Classic oxford",
                "Full description",
                categoryId,
                "PMRG",
                new BigDecimal("1299.00"),
                null,
                List.of("cotton", "formal"),
                List.of(variantReq)
        );
    }

    @Test
    @DisplayName("createProduct - with valid request creates product and variants")
    void createProduct_validRequest_success() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productVariantRepository.existsBySku("OXF-SLIM-BLU-M")).thenReturn(false);
        when(productRepository.existsBySlug("slim-fit-oxford-shirt")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductDetailResponse response = productService.createProduct(createRequest);

        assertNotNull(response);
        assertEquals("Slim Fit Oxford Shirt", response.title());
        verify(productRepository).save(any(Product.class));
        verify(productVariantRepository).save(any(ProductVariant.class));
    }

    @Test
    @DisplayName("createProduct - when SKU already exists throws DuplicateSkuException")
    void createProduct_duplicateSku_throwsException() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productVariantRepository.existsBySku("OXF-SLIM-BLU-M")).thenReturn(true);

        assertThrows(DuplicateSkuException.class, () -> productService.createProduct(createRequest));
    }

    @Test
    @DisplayName("createProduct - when category not found throws CategoryNotFoundException")
    void createProduct_categoryNotFound_throwsException() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> productService.createProduct(createRequest));
    }

    @Test
    @DisplayName("deleteProduct - sets status to ARCHIVED instead of hard delete")
    void deleteProduct_softDeletesProduct() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.deleteProduct(productId);

        assertEquals(ProductStatus.ARCHIVED, product.getStatus());
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("getProducts - with criteria returns PageResponse")
    void getProducts_withCriteria_returnsPage() {
        ProductFilterCriteria criteria = new ProductFilterCriteria(
                categoryId, "PMRG", null, null, null, ProductStatus.PUBLISHED, "Oxford"
        );
        Pageable pageable = PageRequest.of(0, 20);

        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product), pageable, 1));

        PageResponse<ProductSummaryResponse> response = productService.getProducts(criteria, pageable);

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals("Slim Fit Oxford Shirt", response.content().get(0).title());
    }

    @Test
    @DisplayName("getBySlug - when product not found throws ProductNotFoundException")
    void getBySlug_notFound_throwsException() {
        when(productRepository.findWithDetailsBySlug("non-existent-slug")).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.getBySlug("non-existent-slug"));
    }
}
