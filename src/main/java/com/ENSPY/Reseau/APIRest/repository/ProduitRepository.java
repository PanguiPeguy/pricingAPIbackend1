package com.ENSPY.Reseau.APIRest.repository;

import com.ENSPY.Reseau.APIRest.model.Produit;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ProduitRepository extends ReactiveCrudRepository<Produit, UUID> {
    Flux<Produit> findByUserIdOrderByUpdatedAtDesc(UUID userId);
    Mono<Void> deleteByUserId(UUID userId);
    @Query("SELECT * FROM produits WHERE id = CAST(:id AS uuid)")
    Mono<Produit> findById(String id);
}
