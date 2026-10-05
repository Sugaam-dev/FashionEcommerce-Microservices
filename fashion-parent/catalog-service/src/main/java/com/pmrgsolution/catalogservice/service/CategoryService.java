package com.pmrgsolution.catalogservice.service;

import com.pmrgsolution.catalogservice.dto.request.CategoryRequest;
import com.pmrgsolution.catalogservice.dto.response.CategoryResponse;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    List<CategoryResponse> getCategoryTree();

    List<CategoryResponse> getAllRootCategories();

    CategoryResponse getBySlug(String slug);

    CategoryResponse getById(UUID id);

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(UUID id, CategoryRequest request);

    void deleteCategory(UUID id);

    void reorderCategories(List<UUID> orderedIds);
}
