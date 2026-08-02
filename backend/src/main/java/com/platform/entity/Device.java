package com.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "public_ip", nullable = false, unique = true)
    private String publicIp;

    @Column(name = "private_ip")
    private String privateIp;

    @Column(name = "last_heartbeat")
    @CreationTimestamp
    private Instant lastHeartbeat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.IDLE;

    @Column(name = "current_load")
    private Integer currentLoad = 0;

    @Column(name = "region")
    private String region;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.currentLoad == null) {
            this.currentLoad = 0;
        }
        if (this.status == null) {
            this.status = Status.IDLE;
        }
    }

    public enum Status {
        IDLE, BUSY, OFFLINE
    }
}
