package com.pmrgsolution.catalogservice.service.impl;

import com.pmrgsolution.catalogservice.dto.request.ProductCreateRequest;
import com.pmrgsolution.catalogservice.dto.request.ProductFilterCriteria;
import com.pmrgsolution.catalogservice.dto.request.ProductUpdateRequest;
import com.pmrgsolution.catalogservice.dto.request.VariantCreateRequest;
import com.pmrgsolution.catalogservice.dto.response.ImageUploadResponse;
import com.pmrgsolution.catalogservice.dto.response.ProductDetailResponse;
import com.pmrgsolution.catalogservice.dto.response.ProductSummaryResponse;
import com.pmrgsolution.catalogservice.dto.response.VariantResponse;
import com.pmrgsolution.catalogservice.entity.Category;
import com.pmrgsolution.catalogservice.entity.Product;
import com.pmrgsolution.catalogservice.entity.ProductVariant;
import com.pmrgsolution.catalogservice.enums.ProductStatus;
import com.pmrgsolution.catalogservice.exception.CategoryNotFoundException;
import com.pmrgsolution.catalogservice.exception.DuplicateSkuException;
import com.pmrgsolution.catalogservice.exception.ProductNotFoundException;
import com.pmrgsolution.catalogservice.exception.SlugAlreadyExistsException;
import com.pmrgsolution.catalogservice.mapper.ProductMapper;
import com.pmrgsolution.catalogservice.mapper.ProductVariantMapper;
import com.pmrgsolution.catalogservice.repository.CategoryRepository;
import com.pmrgsolution.catalogservice.repository.ProductRepository;
import com.pmrgsolution.catalogservice.repository.ProductVariantRepository;
import com.pmrgsolution.catalogservice.service.ProductService;
import com.pmrgsolution.catalogservice.service.S3StorageService;
import com.pmrgsolution.catalogservice.specification.ProductSpecification;
import com.pmrgsolution.shared.core.exception.ResourceNotFoundException;
import com.pmrgsolution.shared.core.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final S3StorageService s3StorageService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> getProducts(ProductFilterCriteria criteria, Pageable pageable) {
        int boundedPageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Pageable cappedPageable = PageRequest.of(pageable.getPageNumber(), boundedPageSize, pageable.getSort());

        Specification<Product> spec = ProductSpecification.build(criteria);
        Page<Product> productsPage = productRepository.findAll(spec, cappedPageable);

        Page<ProductSummaryResponse> responsePage = productsPage.map(ProductMapper::toSummaryResponse);
        return PageResponse.from(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getBySlug(String slug) {
        Product product = productRepository.findWithDetailsBySlug(slug)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with slug: " + slug));

        List<ProductVariant> variants = productVariantRepository.findByProductIdAndIsActiveTrue(product.getId());
        return ProductMapper.toDetailResponse(product, variants);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getById(UUID id) {
        Product product = productRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + id));

        List<ProductVariant> variants = productVariantRepository.findByProductId(product.getId());
        return ProductMapper.toDetailResponse(product, variants);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductDetailResponse createProduct(ProductCreateRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with ID: " + request.categoryId()));

        // Validate SKU uniqueness across all requested variants
        for (VariantCreateRequest vReq : request.variants()) {
            if (productVariantRepository.existsBySku(vReq.sku())) {
                throw new DuplicateSkuException("SKU already exists: " + vReq.sku());
            }
        }

        String baseSlug = toSlug(request.title());
        String slug = baseSlug;
        int counter = 1;
        while (productRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }

        Product product = ProductMapper.toEntity(request, category, slug);
        Product savedProduct = productRepository.save(product);

        List<ProductVariant> savedVariants = new ArrayList<>();
        for (VariantCreateRequest vReq : request.variants()) {
            ProductVariant variant = ProductVariantMapper.toEntity(vReq, savedProduct);
            savedVariants.add(productVariantRepository.save(variant));
        }

        savedProduct.setVariants(savedVariants);
        log.info("Created product id={}, slug={}, variantsCount={}", savedProduct.getId(), savedProduct.getSlug(), savedVariants.size());
        return ProductMapper.toDetailResponse(savedProduct, savedVariants);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductDetailResponse updateProduct(UUID id, ProductUpdateRequest request) {
        Product product = productRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + id));

        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new CategoryNotFoundException("Category not found with ID: " + request.categoryId()));
        }

        ProductMapper.updateEntity(product, request, category);
        Product updated = productRepository.save(product);

        List<ProductVariant> variants = productVariantRepository.findByProductId(id);
        log.info("Updated product id={}, slug={}", updated.getId(), updated.getSlug());
        return ProductMapper.toDetailResponse(updated, variants);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public VariantResponse addVariant(UUID productId, VariantCreateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + productId));

        if (productVariantRepository.existsBySku(request.sku())) {
            throw new DuplicateSkuException("SKU already exists: " + request.sku());
        }

        ProductVariant variant = ProductVariantMapper.toEntity(request, product);
        ProductVariant saved = productVariantRepository.save(variant);

        log.info("Added variant id={}, sku={} to product id={}", saved.getId(), saved.getSku(), productId);
        return ProductVariantMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public VariantResponse updateVariant(UUID productId, UUID variantId, VariantCreateRequest request) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with ID: " + variantId));

        if (!variant.getProduct().getId().equals(productId)) {
            throw new ResourceNotFoundException("Variant does not belong to product with ID: " + productId);
        }

        if (!variant.getSku().equalsIgnoreCase(request.sku().trim()) && productVariantRepository.existsBySku(request.sku())) {
            throw new DuplicateSkuException("SKU already exists: " + request.sku());
        }

        variant.setSku(request.sku().trim());
        variant.setSize(request.size() != null ? request.size().trim() : null);
        variant.setColor(request.color() != null ? request.color().trim() : null);
        variant.setColorHex(request.colorHex() != null ? request.colorHex().trim() : null);
        variant.setPrice(request.price());
        variant.setCompareAtPrice(request.compareAtPrice());
        variant.setVariantImageUrl(request.variantImageUrl());
        variant.setActive(request.isActive());

        ProductVariant saved = productVariantRepository.save(variant);
        return ProductVariantMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deleteVariant(UUID productId, UUID variantId) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with ID: " + variantId));

        if (!variant.getProduct().getId().equals(productId)) {
            throw new ResourceNotFoundException("Variant does not belong to product with ID: " + productId);
        }

        productVariantRepository.delete(variant);
        log.info("Deleted variant id={} from product id={}", variantId, productId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public List<String> uploadProductImages(UUID productId, List<MultipartFile> files) {
        Product product = productRepository.findWithDetailsById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + productId));

        if (files == null || files.isEmpty()) {
            return product.getImageUrls();
        }

        List<String> uploadedUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            ImageUploadResponse uploadResponse = s3StorageService.uploadImage(file, "products/" + productId);
            uploadedUrls.add(uploadResponse.originalUrl());
        }

        List<String> currentImages = product.getImageUrls() != null ? new ArrayList<>(product.getImageUrls()) : new ArrayList<>();
        currentImages.addAll(uploadedUrls);
        product.setImageUrls(currentImages);

        if (product.getPrimaryImageUrl() == null || product.getPrimaryImageUrl().isBlank()) {
            if (!uploadedUrls.isEmpty()) {
                product.setPrimaryImageUrl(uploadedUrls.get(0));
            }
        }

        productRepository.save(product);
        log.info("Uploaded {} images for product id={}", uploadedUrls.size(), productId);
        return product.getImageUrls();
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + id));

        // Soft delete: transition status to ARCHIVED
        product.setStatus(ProductStatus.ARCHIVED);
        productRepository.save(product);
        log.info("Soft deleted product id={} (status=ARCHIVED)", id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductDetailResponse updateProductStatus(UUID id, ProductStatus status) {
        Product product = productRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + id));

        product.setStatus(status);
        Product saved = productRepository.save(product);
        List<ProductVariant> variants = productVariantRepository.findByProductId(id);
        log.info("Updated product id={} status to {}", id, status);
        return ProductMapper.toDetailResponse(saved, variants);
    }

    private String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return UUID.randomUUID().toString().substring(0, 8);
        }
        String nowhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH).replaceAll("-+", "-").replaceAll("^-|-$", "");
    }
}
