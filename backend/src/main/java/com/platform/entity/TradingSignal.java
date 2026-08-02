package com.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trading_signals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradingSignal {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 10)
    private Action action;

    @Column(name = "confidence", precision = 5, scale = 2)
    private BigDecimal confidence;

    @Column(name = "price_target", precision = 12, scale = 2)
    private BigDecimal priceTarget;

    @CreationTimestamp
    @Column(name = "generated_at", updatable = false)
    private Instant generatedAt;

    @Column(name = "executed")
    private Boolean executed = false;

    @Column(name = "execution_price", precision = 12, scale = 2)
    private BigDecimal executionPrice;

    @Column(name = "executed_at")
    private Instant executedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.executed == null) {
            this.executed = false;
        }
    }

    public enum Action {
        BUY, SELL, HOLD
    }
}
