package com.commercesuite.catalog;

import com.commercesuite.AbstractIT;
import com.commercesuite.catalog.dto.UpsertCategoryRequest;
import com.commercesuite.catalog.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class CategoryHierarchyIT extends AbstractIT {
    @Autowired CategoryService categories;

    @Test void buildsTreeAndPreventsSelfParent() {
        var root = categories.create(new UpsertCategoryRequest(null, "Electronics " + System.nanoTime(),
                null, null, null, 0, true));
        var child = categories.create(new UpsertCategoryRequest(root.id(), "Phones " + System.nanoTime(),
                null, null, null, 0, true));
        var tree = categories.tree();
        assertFalse(tree.isEmpty());
        assertTrue(tree.stream().anyMatch(c -> c.id().equals(root.id())
                && c.children().stream().anyMatch(cc -> cc.id().equals(child.id()))));

        assertThrows(RuntimeException.class, () -> categories.update(root.id(),
                new UpsertCategoryRequest(root.id(), root.name(), null, null, null, 0, true)));
    }
}