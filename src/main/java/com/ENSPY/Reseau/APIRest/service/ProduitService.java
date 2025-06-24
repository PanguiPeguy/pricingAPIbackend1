package com.ENSPY.Reseau.APIRest.service;

import com.ENSPY.Reseau.APIRest.model.Produit;
import com.ENSPY.Reseau.APIRest.repository.ProduitRepository;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ProduitService {

    private final ProduitRepository produitRepository;
    private final AuthService authService;

    public ProduitService(ProduitRepository produitRepository, AuthService authService) {
        this.produitRepository = produitRepository;
        this.authService = authService;
    }

    public Flux<Produit> getAllProducts() {
        return authService.getUserFromContext()
                .flatMapMany(user -> produitRepository.findByUserIdOrderByUpdatedAtDesc(user.getId()));
    }

    public Mono<Produit> getProductById(UUID id) {
        System.out.println("=== DEBUG GET PRODUCT BY ID ===");
        System.out.println("ID reçu: " + id);
        System.out.println("Type de l'ID: " + id.getClass().getSimpleName());

        return authService.getUserFromContext()
                .flatMap(currentUser -> produitRepository.findById(id)
                        .filter(produit -> produit.getUserId().equals(currentUser.getId()))
                        .switchIfEmpty(Mono.error(new RuntimeException("Produit non trouvé ou accès non autorisé")))
                        .doOnNext(produit -> {
                            System.out.println("Produit trouvé: " + produit.getName());
                            System.out.println("ID du produit trouvé: " + produit.getId());
                        })
                        .doOnError(error -> {
                            System.err.println("Erreur lors de la recherche: " + error.getMessage());
                            error.printStackTrace();
                        }));
    }

    public Mono<Produit> createProduct(Produit produit) {
        return ReactiveSecurityContextHolder.getContext()
                .flatMap(securityContext -> {
                    if (securityContext.getAuthentication() == null) {
                        return Mono.error(new RuntimeException("Utilisateur non authentifié"));
                    }
                    return authService.getUserFromContext()
                            .map(user -> {
                                produit.setUserId(user.getId());
                                produit.setCreatedAt(LocalDateTime.now());
                                produit.setUpdatedAt(LocalDateTime.now());
                                return produit;
                            });
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Contexte de sécurité non disponible")))
                .flatMap(produitRepository::save)
                .doOnNext(savedProduit -> {
                    System.out.println("=== PRODUIT SAUVEGARDÉ ===");
                    System.out.println("ID: " + savedProduit.getId());
                    System.out.println("UserId: " + savedProduit.getUserId());
                    System.out.println("Marge désirée sauvegardée: " + savedProduit.getDesiredMargin());
                    System.out.println("==========================");
                });
    }

    public Mono<Produit> updateProduct(UUID id, Produit produitDetails) {
        return getProductById(id)
                .flatMap(produit -> {
                    if (produitDetails.getName() != null) {
                        produit.setName(produitDetails.getName());
                    }
                    if (produitDetails.getDescription() != null) {
                        produit.setDescription(produitDetails.getDescription());
                    }
                    if (produitDetails.getPrixDesConcurrents() != null) {
                        produit.setPrixDesConcurrents(produitDetails.getPrixDesConcurrents());
                    }
                    if (produitDetails.getCoutDeProduction() != null) {
                        produit.setCoutDeProduction(produitDetails.getCoutDeProduction());
                    }
                    if (produitDetails.getCategory() != null) {
                        produit.setCategory(produitDetails.getCategory());
                    }
                    if (produitDetails.getType() != null) {
                        produit.setType(produitDetails.getType());
                    }
                    if (produitDetails.getStock() != null) {
                        produit.setStock(produitDetails.getStock());
                    }
                    if (produitDetails.getDesiredMargin() != null) {
                        produit.setDesiredMargin(produitDetails.getDesiredMargin());
                    }
                    produit.setUpdatedAt(LocalDateTime.now());
                    return produitRepository.save(produit)
                            .doOnNext(updatedProduit -> {
                                System.out.println("=== PRODUIT MIS À JOUR ===");
                                System.out.println("Marge désirée finale: " + updatedProduit.getDesiredMargin());
                                System.out.println("===========================");
                            });
                });
    }

    public Mono<Void> deleteProduct(UUID id) {
        return getProductById(id)
                .flatMap(produitRepository::delete);
    }
}