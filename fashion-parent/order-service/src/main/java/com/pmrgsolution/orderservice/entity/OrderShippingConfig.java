package com.pmrgsolution.orderservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_shipping_config")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderShippingConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "default_shipping_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal defaultShippingFee;

    @Column(name = "free_shipping_threshold", nullable = false, precision = 10, scale = 2)
    private BigDecimal freeShippingThreshold;

    @Column(name = "estimated_delivery_min_days", nullable = false)
    private int estimatedDeliveryMinDays;

    @Column(name = "estimated_delivery_max_days", nullable = false)
    private int estimatedDeliveryMaxDays;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderShippingConfig other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
