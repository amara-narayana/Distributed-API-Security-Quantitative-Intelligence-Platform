package com.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "extracted_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtractedData {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "source_domain", nullable = false, length = 255)
    private String sourceDomain;

    @Column(name = "product_id", length = 100)
    private String productId;

    @Column(name = "product_name", length = 500)
    private String productName;

    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "currency", length = 3)
    private String currency = "USD";

    @Column(name = "inventory_level")
    private Integer inventoryLevel;

    @Column(name = "shipping_date")
    private LocalDate shippingDate;

    @CreationTimestamp
    @Column(name = "extracted_at", updatable = false)
    private Instant extractedAt;

    @Column(name = "raw_data", columnDefinition = "jsonb")
    private String rawData;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.currency == null) {
            this.currency = "USD";
        }
    }
}
