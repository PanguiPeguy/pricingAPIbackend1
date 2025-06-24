package com.ENSPY.Reseau.APIRest.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@Table("optimal_prices")
public class OptimalPriceResult {

    @Id
    private UUID id;

    @Column("product_id")
    private UUID productId;

    @Column("product_name")
    private String productName;

    @Column("prix_des_concurrents")
    private Double prixDesConcurrents;

    @Column("optimal_price")
    private Double optimalPrice;

    @Column("potential_revenue")
    private Double potentialRevenue;

    @Column("margin")
    private Double margin;

    @Column("user_id")
    private UUID userId;

    @Column("calculated_at")
    private LocalDateTime calculatedAt;

    public OptimalPriceResult() {
    }

    public OptimalPriceResult(UUID id, UUID productId, String productName, Double basePrice, Double optimalPrice, Double potentialRevenue, Double margin, UUID userId, LocalDateTime calculatedAt) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.prixDesConcurrents = basePrice;
        this.optimalPrice = optimalPrice;
        this.potentialRevenue = potentialRevenue;
        this.margin = margin;
        this.userId = userId;
        this.calculatedAt = calculatedAt;
    }
}
