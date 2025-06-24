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
@Table("tarification_prices")
public class TarificationResult {

    @Id
    private UUID id;

    @Column("product_id")
    private UUID productId;

    @Column("product_name")
    private String productName;

    @Column("prix_des_concurrents")
    private Double prixDesConcurrents;

    @Column("tarification_price")
    private Double tarificationPrice;

    @Column("potential_revenue")
    private Double potentialRevenue;

    @Column("margin")
    private Double margin;

    @Column("user_id")
    private UUID userId;

    @Column("time_in_months")
    private Double timeInMonths;

    @Column("calculated_at")
    private LocalDateTime calculatedAt;

    public TarificationResult() {
    }

    public TarificationResult(UUID id,Double timeInMonths, UUID productId, String productName, Double prixDesConcurrents, Double tarificationPrice, Double potentialRevenue, Double margin, UUID userId, LocalDateTime calculatedAt) {
        this.id = id;
        this.productId = productId;
        this.timeInMonths = timeInMonths;
        this.productName = productName;
        this.prixDesConcurrents = prixDesConcurrents;
        this.tarificationPrice = tarificationPrice;
        this.potentialRevenue = potentialRevenue;
        this.margin = margin;
        this.userId = userId;
        this.calculatedAt = calculatedAt;
    }
}
