package com.commercesuite.inventory.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Concurrency-safe stock allocator.
 *
 * Acquires a transactional PostgreSQL advisory lock keyed on the variant id
 * so that concurrent reserve/commit/release flows for the same variant are
 * serialized. The lock is released automatically at COMMIT/ROLLBACK.
 */
@Component
public class InventoryAllocator {

    @PersistenceContext
    private EntityManager em;

    /** Must be called inside an active transaction. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void acquireVariantLock(UUID variantId) {
        em.createNativeQuery("SELECT pg_advisory_xact_lock(hashtext('inv:' || cast(:vid as text)))")
                .setParameter("vid", variantId.toString())
                .getSingleResult();
    }
}