package com.commercesuite.vendor.entity;

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
@Table(name = "vendor_profiles")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE vendor_profiles SET deleted_at = now(), updated_at = now() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class VendorProfile extends AuditableEntity {

    @Column(name = "vendor_id", nullable = false, unique = true)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID vendorId;

    @Column(name = "store_name", nullable = false, length = 120) private String storeName;
    @Column(name = "store_slug", nullable = false, unique = true, length = 140) private String storeSlug;
    @Column(name = "description", columnDefinition = "text")     private String description;
    @Column(name = "logo_url")     private String logoUrl;
    @Column(name = "banner_url")   private String bannerUrl;
    @Column(name = "support_email", length = 255) private String supportEmail;
    @Column(name = "support_phone", length = 20)  private String supportPhone;
    @Column(name = "website_url")  private String websiteUrl;
    @Column(name = "return_policy", columnDefinition = "text") private String returnPolicy;
}