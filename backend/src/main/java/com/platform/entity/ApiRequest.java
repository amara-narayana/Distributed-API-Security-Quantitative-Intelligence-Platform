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
@Table(name = "api_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiRequest {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "target_url", nullable = false, length = 2048)
    private String targetUrl;

    @Column(name = "method", nullable = false, length = 10)
    private String method;

    @Column(name = "request_headers", columnDefinition = "jsonb")
    private String requestHeaders;

    @Column(name = "request_body", columnDefinition = "text")
    private String requestBody;

    @Column(name = "response_body", columnDefinition = "text")
    private String responseBody;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @CreationTimestamp
    @Column(name = "timestamp", updatable = false)
    private Instant timestamp;

    @Column(name = "payload_hash", length = 64)
    private String payloadHash;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
