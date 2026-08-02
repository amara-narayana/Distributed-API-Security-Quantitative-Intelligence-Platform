package com.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "entry_id", unique = true, nullable = false, length = 100)
    private String entryId;

    @CreationTimestamp
    @Column(name = "timestamp", updatable = false)
    private Instant timestamp;

    @Column(name = "actor", nullable = false, length = 255)
    private String actor;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "target_resource", length = 500)
    private String targetResource;

    @Column(name = "result_hash", length = 64)
    private String resultHash;

    @Column(name = "blockchain_tx_id", length = 100)
    private String blockchainTxId;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
