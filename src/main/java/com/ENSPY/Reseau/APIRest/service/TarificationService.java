package com.ENSPY.Reseau.APIRest.service;

import com.ENSPY.Reseau.APIRest.model.Produit;
import com.ENSPY.Reseau.APIRest.model.TarificationResult;
import com.ENSPY.Reseau.APIRest.model.User;
import com.ENSPY.Reseau.APIRest.repository.TarificationRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class TarificationService {

    private final TarificationRepository tarificationRepository;
    private final ProduitService produitService;
    private final AuthService authService;

    public TarificationService(
            TarificationRepository tarificationRepository,
            ProduitService produitService,
            AuthService authService) {
        this.tarificationRepository = tarificationRepository;
        this.produitService = produitService;
        this.authService = authService;
    }

    public Mono<TarificationResult> calculateDecremageTarificationPrice(UUID productId, Integer prixMax) {
        return produitService.getProductById(productId)
                .zipWith(authService.getUserFromContext())
                .flatMap(tuple -> {
                    Produit produit = tuple.getT1();
                    User currentUser = tuple.getT2();
                    double alpha = 0.09;
                    double tarificationPrice = prixMax * Math.exp(-alpha);

                    if (tarificationPrice < produit.getCoutDeProduction()) {
                        tarificationPrice = produit.getCoutDeProduction() * 1.2;
                    }

                    double potentialRevenue = tarificationPrice * produit.getStock();
                    double margin = (tarificationPrice - produit.getCoutDeProduction()) / tarificationPrice * 100;

                    TarificationResult result = new TarificationResult();
                    result.setProductId(produit.getId());
                    result.setProductName(produit.getName());
                    result.setPrixDesConcurrents(produit.getPrixDesConcurrents());
                    result.setTarificationPrice(tarificationPrice);
                    result.setPotentialRevenue(potentialRevenue);
                    result.setMargin(margin);
                    result.setUserId(currentUser.getId());

                    return tarificationRepository.save(result);
                });
    }

    public Mono<TarificationResult> calculatePenetrationTarificationPrice(UUID productId, Integer prixMin) {
        return produitService.getProductById(productId)
                .zipWith(authService.getUserFromContext())
                .flatMap(tuple -> {
                    Produit produit = tuple.getT1();
                    User currentUser = tuple.getT2();
                    double beta = 0.055 * produit.getCoutDeProduction();
                    double tarificationPrice = prixMin + beta * Math.log(10);

                    if (tarificationPrice < prixMin) {
                        tarificationPrice = prixMin;
                    }

                    double potentialRevenue = tarificationPrice * produit.getStock();
                    double margin = (tarificationPrice - produit.getCoutDeProduction()) / tarificationPrice * 100;

                    TarificationResult result = new TarificationResult();
                    result.setProductId(produit.getId());
                    result.setProductName(produit.getName());
                    result.setPrixDesConcurrents(produit.getPrixDesConcurrents());
                    result.setTarificationPrice(tarificationPrice);
                    result.setPotentialRevenue(potentialRevenue);
                    result.setMargin(margin);
                    result.setUserId(currentUser.getId());

                    return tarificationRepository.save(result);
                });
    }

    public Mono<TarificationResult> calculateAlignementTarificationPrice(UUID productId) {
        return produitService.getProductById(productId)
                .zipWith(authService.getUserFromContext())
                .flatMap(tuple -> {
                    Produit produit = tuple.getT1();
                    User currentUser = tuple.getT2();
                    Double prixConcurrents = produit.getPrixDesConcurrents();
                    double delta = 0.015 * prixConcurrents;
                    double tarificationPrice = prixConcurrents + delta;

                    if (tarificationPrice < produit.getCoutDeProduction()) {
                        tarificationPrice = produit.getCoutDeProduction() * 1.1;
                    }

                    double potentialRevenue = tarificationPrice * produit.getStock();
                    double margin = (tarificationPrice - produit.getCoutDeProduction()) / tarificationPrice * 100;

                    TarificationResult result = new TarificationResult();
                    result.setProductId(produit.getId());
                    result.setProductName(produit.getName());
                    result.setPrixDesConcurrents(produit.getPrixDesConcurrents());
                    result.setTarificationPrice(tarificationPrice);
                    result.setPotentialRevenue(potentialRevenue);
                    result.setMargin(margin);
                    result.setUserId(currentUser.getId());

                    return tarificationRepository.save(result);
                });
    }

    public Flux<TarificationResult> getPricingHistory() {
        return authService.getUserFromContext()
                .flatMapMany(user -> tarificationRepository.findByUserIdOrderByCalculatedAtDesc(user.getId()));
    }
}
