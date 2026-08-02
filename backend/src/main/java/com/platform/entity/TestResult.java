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
@Table(name = "test_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestResult {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "test_type", nullable = false, length = 50)
    private String testType;

    @Column(name = "endpoint", nullable = false, length = 2048)
    private String endpoint;

    @Column(name = "vulnerability_found")
    private Boolean vulnerabilityFound = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 20)
    private Severity severity;

    @Column(name = "details", columnDefinition = "jsonb")
    private String details;

    @CreationTimestamp
    @Column(name = "timestamp", updatable = false)
    private Instant timestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.vulnerabilityFound == null) {
            this.vulnerabilityFound = false;
        }
    }

    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
