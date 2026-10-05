package com.pmrgsolution.catalogservice.specification;

import com.pmrgsolution.catalogservice.dto.request.ProductFilterCriteria;
import com.pmrgsolution.catalogservice.entity.Product;
import com.pmrgsolution.catalogservice.enums.ProductStatus;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ProductSpecification {

    private ProductSpecification() {
    }

    public static Specification<Product> withCategory(UUID categoryId) {
        return (root, query, cb) -> categoryId == null ? cb.conjunction()
                : cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> withBrand(String brand) {
        return (root, query, cb) -> (brand == null || brand.isBlank()) ? cb.conjunction()
                : cb.equal(cb.lower(root.get("brand")), brand.trim().toLowerCase());
    }

    public static Specification<Product> withMinPrice(BigDecimal min) {
        return (root, query, cb) -> min == null ? cb.conjunction()
                : cb.greaterThanOrEqualTo(
                cb.coalesce(root.get("salePrice"), root.get("basePrice")),
                min
        );
    }

    public static Specification<Product> withMaxPrice(BigDecimal max) {
        return (root, query, cb) -> max == null ? cb.conjunction()
                : cb.lessThanOrEqualTo(
                cb.coalesce(root.get("salePrice"), root.get("basePrice")),
                max
        );
    }

    public static Specification<Product> withMinRating(Double min) {
        return (root, query, cb) -> min == null ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("averageRating"), min);
    }

    public static Specification<Product> withStatus(ProductStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    public static Specification<Product> withKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            if (query != null) {
                query.distinct(true);
            }
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
            Predicate brandMatch = cb.like(cb.lower(root.get("brand")), pattern);
            Predicate descMatch = cb.like(cb.lower(root.get("shortDescription")), pattern);
            jakarta.persistence.criteria.Join<Product, String> tagsJoin = root.join("tags", JoinType.LEFT);
            Predicate tagMatch = cb.like(cb.lower(tagsJoin), pattern);

            return cb.or(titleMatch, brandMatch, descMatch, tagMatch);
        };
    }

    public static Specification<Product> build(ProductFilterCriteria criteria) {
        if (criteria == null) {
            return Specification.where(null);
        }
        List<Specification<Product>> specs = new ArrayList<>();

        if (criteria.categoryId() != null) {
            specs.add(withCategory(criteria.categoryId()));
        }
        if (criteria.brand() != null && !criteria.brand().isBlank()) {
            specs.add(withBrand(criteria.brand()));
        }
        if (criteria.minPrice() != null) {
            specs.add(withMinPrice(criteria.minPrice()));
        }
        if (criteria.maxPrice() != null) {
            specs.add(withMaxPrice(criteria.maxPrice()));
        }
        if (criteria.minRating() != null) {
            specs.add(withMinRating(criteria.minRating()));
        }
        if (criteria.status() != null) {
            specs.add(withStatus(criteria.status()));
        }
        if (criteria.search() != null && !criteria.search().isBlank()) {
            specs.add(withKeyword(criteria.search()));
        }

        return specs.stream()
                .reduce(Specification.where(null), Specification::and);
    }
}
