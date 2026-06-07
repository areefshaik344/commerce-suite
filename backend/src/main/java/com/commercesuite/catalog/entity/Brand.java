package com.commercesuite.catalog.entity;

import com.commercesuite.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "brands")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE brands SET deleted_at = now(), updated_at = now() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class Brand extends AuditableEntity {
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, unique = true, length = 140) private String slug;
    @Column(columnDefinition = "text") private String description;
    @Column(name = "logo_url") private String logoUrl;
    @Column(nullable = false) private boolean active;
}