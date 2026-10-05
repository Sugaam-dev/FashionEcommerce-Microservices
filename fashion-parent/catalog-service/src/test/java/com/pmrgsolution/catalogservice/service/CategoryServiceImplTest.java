package com.pmrgsolution.catalogservice.service;

import com.pmrgsolution.catalogservice.dto.request.CategoryRequest;
import com.pmrgsolution.catalogservice.dto.response.CategoryResponse;
import com.pmrgsolution.catalogservice.entity.Category;
import com.pmrgsolution.catalogservice.exception.CategoryNotFoundException;
import com.pmrgsolution.catalogservice.exception.SlugAlreadyExistsException;
import com.pmrgsolution.catalogservice.repository.CategoryRepository;
import com.pmrgsolution.catalogservice.repository.ProductRepository;
import com.pmrgsolution.catalogservice.service.impl.CategoryServiceImpl;
import com.pmrgsolution.shared.core.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
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
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private UUID categoryId;
    private Category category;
    private CategoryRequest request;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        category = Category.builder()
                .id(categoryId)
                .name("Men's Fashion")
                .slug("mens-fashion")
                .description("Men clothing")
                .isActive(true)
                .displayOrder(1)
                .subCategories(Collections.emptyList())
                .build();

        request = new CategoryRequest(
                "Men's Fashion",
                "mens-fashion",
                "Men clothing",
                null,
                "icon.png",
                "banner.png",
                1,
                true
        );
    }

    @Test
    @DisplayName("createCategory - with valid request saves and returns CategoryResponse")
    void createCategory_withValidRequest_success() {
        when(categoryRepository.existsBySlug("mens-fashion")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryResponse response = categoryService.createCategory(request);

        assertNotNull(response);
        assertEquals("Men's Fashion", response.name());
        assertEquals("mens-fashion", response.slug());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("createCategory - when slug already exists throws SlugAlreadyExistsException")
    void createCategory_whenDuplicateSlug_throwsException() {
        when(categoryRepository.existsBySlug("mens-fashion")).thenReturn(true);

        assertThrows(SlugAlreadyExistsException.class, () -> categoryService.createCategory(request));
    }

    @Test
    @DisplayName("updateCategory - when category not found throws CategoryNotFoundException")
    void updateCategory_whenNotFound_throwsException() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> categoryService.updateCategory(categoryId, request));
    }

    @Test
    @DisplayName("deleteCategory - when products exist throws ConflictException")
    void deleteCategory_whenProductsExist_throwsConflictException() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.existsByCategoryId(categoryId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> categoryService.deleteCategory(categoryId));
    }

    @Test
    @DisplayName("deleteCategory - when subcategories exist throws ConflictException")
    void deleteCategory_whenSubcategoriesExist_throwsConflictException() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.existsByCategoryId(categoryId)).thenReturn(false);
        when(categoryRepository.countByParentId(categoryId)).thenReturn(2L);

        assertThrows(ConflictException.class, () -> categoryService.deleteCategory(categoryId));
    }

    @Test
    @DisplayName("getCategoryTree - returns list of root active categories")
    void getCategoryTree_returnsCategories() {
        when(categoryRepository.findRootActiveCategories()).thenReturn(List.of(category));

        List<CategoryResponse> tree = categoryService.getCategoryTree();

        assertNotNull(tree);
        assertEquals(1, tree.size());
        assertEquals("Men's Fashion", tree.get(0).name());
    }
}
