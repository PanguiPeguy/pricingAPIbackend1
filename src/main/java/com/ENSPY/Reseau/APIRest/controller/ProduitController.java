package com.ENSPY.Reseau.APIRest.controller;

import com.ENSPY.Reseau.APIRest.model.Produit;
import com.ENSPY.Reseau.APIRest.service.PricingService;
import com.ENSPY.Reseau.APIRest.service.ProduitService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@Controller
public class ProduitController {

    private final ProduitService produitService;
    private final PricingService pricingService;

    public ProduitController(ProduitService produitService, PricingService pricingService) {
        this.produitService = produitService;
        this.pricingService = pricingService;
    }

    public Mono<ServerResponse> getAllProducts() {
        return ServerResponse.ok()
                .body(produitService.getAllProducts(), Produit.class)
                .onErrorResume(e -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .bodyValue(Map.of("message", e.getMessage())));
    }

    public Mono<ServerResponse> getProductById(UUID id) {
        return produitService.getProductById(id)
                .flatMap(produit -> ServerResponse.ok().bodyValue(produit))
                .onErrorResume(e -> ServerResponse.status(HttpStatus.BAD_REQUEST)
                        .bodyValue(Map.of("message", e.getMessage())));
    }

    public Mono<ServerResponse> createProduct(Produit produit) {
        return produitService.createProduct(produit)
                .flatMap(newProduit -> ServerResponse.status(HttpStatus.CREATED).bodyValue(newProduit))
                .onErrorResume(e -> ServerResponse.status(HttpStatus.BAD_REQUEST)
                        .bodyValue(Map.of("message", e.getMessage())));
    }

    public Mono<ServerResponse> updateProduct(UUID id, Produit produit) {
        return produitService.updateProduct(id, produit)
                .flatMap(updatedProduit -> ServerResponse.ok().bodyValue(updatedProduit))
                .onErrorResume(e -> ServerResponse.status(HttpStatus.BAD_REQUEST)
                        .bodyValue(Map.of("message", e.getMessage())));
    }

    public Mono<ServerResponse> deleteProduct(UUID id) {
        return produitService.deleteProduct(id)
                .then(ServerResponse.ok().bodyValue(Map.of("message", "Produit supprimé avec succès")))
                .onErrorResume(e -> ServerResponse.status(HttpStatus.BAD_REQUEST)
                        .bodyValue(Map.of("message", e.getMessage())));
    }
}