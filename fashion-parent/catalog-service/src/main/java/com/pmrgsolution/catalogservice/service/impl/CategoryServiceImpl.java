package com.pmrgsolution.catalogservice.service.impl;

import com.pmrgsolution.catalogservice.dto.request.CategoryRequest;
import com.pmrgsolution.catalogservice.dto.response.CategoryResponse;
import com.pmrgsolution.catalogservice.entity.Category;
import com.pmrgsolution.catalogservice.exception.CategoryNotFoundException;
import com.pmrgsolution.catalogservice.exception.SlugAlreadyExistsException;
import com.pmrgsolution.catalogservice.mapper.CategoryMapper;
import com.pmrgsolution.catalogservice.repository.CategoryRepository;
import com.pmrgsolution.catalogservice.repository.ProductRepository;
import com.pmrgsolution.catalogservice.service.CategoryService;
import com.pmrgsolution.shared.core.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "'tree'")
    public List<CategoryResponse> getCategoryTree() {
        log.debug("Fetching root active category tree from database (cache miss)");
        List<Category> rootCategories = categoryRepository.findRootActiveCategories();
        return rootCategories.stream()
                .map(CategoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllRootCategories() {
        List<Category> rootCategories = categoryRepository.findAllRootCategories();
        return rootCategories.stream()
                .map(CategoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with slug: " + slug));
        return CategoryMapper.toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with ID: " + id));
        return CategoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse createCategory(CategoryRequest request) {
        String slug = (request.slug() != null && !request.slug().isBlank())
                ? toSlug(request.slug())
                : toSlug(request.name());

        if (categoryRepository.existsBySlug(slug)) {
            throw new SlugAlreadyExistsException("Category slug already in use: " + slug);
        }

        Category parent = null;
        if (request.parentId() != null) {
            parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new CategoryNotFoundException("Parent category not found with ID: " + request.parentId()));
        }

        Category category = CategoryMapper.toEntity(request, parent);
        category.setSlug(slug);

        Category saved = categoryRepository.save(category);
        log.info("Created category id={}, slug={}", saved.getId(), saved.getSlug());
        return CategoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse updateCategory(UUID id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with ID: " + id));

        String slug = (request.slug() != null && !request.slug().isBlank())
                ? toSlug(request.slug())
                : toSlug(request.name());

        if (categoryRepository.existsBySlugAndIdNot(slug, id)) {
            throw new SlugAlreadyExistsException("Category slug already in use: " + slug);
        }

        Category parent = null;
        if (request.parentId() != null) {
            if (request.parentId().equals(id)) {
                throw new ConflictException("Category cannot be its own parent");
            }
            parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new CategoryNotFoundException("Parent category not found with ID: " + request.parentId()));
        }

        CategoryMapper.updateEntity(category, request, parent);
        category.setSlug(slug);

        Category updated = categoryRepository.save(category);
        log.info("Updated category id={}, slug={}", updated.getId(), updated.getSlug());
        return CategoryMapper.toResponse(updated);
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with ID: " + id));

        if (productRepository.existsByCategoryId(id)) {
            throw new ConflictException("Cannot delete category because products are assigned to it");
        }

        if (categoryRepository.countByParentId(id) > 0) {
            throw new ConflictException("Cannot delete category with existing subcategories. Reassign or delete subcategories first.");
        }

        categoryRepository.delete(category);
        log.info("Deleted category id={}", id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void reorderCategories(List<UUID> orderedIds) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            return;
        }
        for (int i = 0; i < orderedIds.size(); i++) {
            UUID catId = orderedIds.get(i);
            int order = i;
            categoryRepository.findById(catId).ifPresent(c -> {
                c.setDisplayOrder(order);
                categoryRepository.save(c);
            });
        }
        log.info("Reordered {} categories", orderedIds.size());
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
