package com.commercesuite.catalog.entity;

import com.commercesuite.common.entity.AuditableEntity;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "categories")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE categories SET deleted_at = now(), updated_at = now() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class Category extends AuditableEntity {

    @Column(name = "parent_id") @JdbcTypeCode(SqlTypes.UUID) private UUID parentId;

    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, unique = true, length = 140) private String slug;
    @Column(columnDefinition = "text") private String description;
    @Column(length = 80) private String icon;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(nullable = false) private boolean active;

    @PrePersist void defaults() { /* active default true handled by builder/db */ }
}