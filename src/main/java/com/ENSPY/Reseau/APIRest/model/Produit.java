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
@Table("produits")
public class Produit {

    @Id
    private UUID id;

    @Column("name")
    private String name;

    @Column("description")
    private String description;

    @Column("prix_des_concurrents")
    private Double prixDesConcurrents;

    @Column("cout_de_production")
    private Double coutDeProduction;

    @Column("desired_margin")
    private Double desiredMargin;

    @Column("category")
    private String category;

    @Column("type")
    private String type;

    @Column("stock")
    private Integer stock;

    @Column("user_id")
    private UUID userId;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    public Produit() {
    }

    public Produit(UUID id, String name, String description, Double prixDesConcurrents, Double coutDeProduction, Double desiredMargin, String category, String type, Integer stock, UUID userId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.prixDesConcurrents = prixDesConcurrents;
        this.coutDeProduction = coutDeProduction;
        this.desiredMargin = desiredMargin;
        this.category = category;
        this.type = type;
        this.stock = stock;
        this.userId = userId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
