package com.commercesuite.catalog.service;

import com.commercesuite.catalog.dto.ProductSearchCriteria;
import com.commercesuite.catalog.entity.Product;
import com.commercesuite.catalog.entity.ProductStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** JPA Specifications for catalog search (keyword + category + brand + vendor + status). */
public final class ProductSpecifications {
    private ProductSpecifications() {}

    public static Specification<Product> from(ProductSearchCriteria c) {
        return (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (c.keyword() != null && !c.keyword().isBlank()) {
                String like = "%" + c.keyword().toLowerCase().trim() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("title")),       like),
                        cb.like(cb.lower(root.get("shortDescription")), like),
                        cb.like(cb.lower(root.get("slug")),        like)
                ));
            }
            if (c.categoryId() != null) ps.add(cb.equal(root.get("categoryId"), c.categoryId()));
            if (c.brandId()    != null) ps.add(cb.equal(root.get("brandId"),    c.brandId()));
            if (c.vendorId()   != null) ps.add(cb.equal(root.get("vendorId"),   c.vendorId()));
            if (c.status()     != null) ps.add(cb.equal(root.get("status"),     c.status()));
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    /** Public catalog endpoints only ever return APPROVED products. */
    public static Specification<Product> publicOnly() {
        return (root, q, cb) -> cb.equal(root.get("status"), ProductStatus.APPROVED);
    }
}