package com.ENSPY.Reseau.APIRest.service;

import com.ENSPY.Reseau.APIRest.model.Produit;
import com.ENSPY.Reseau.APIRest.model.TarificationResult;
import com.ENSPY.Reseau.APIRest.model.User;
import com.ENSPY.Reseau.APIRest.repository.TarificationRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

    /**
     * Calcule le temps écoulé depuis la date de lancement du produit en mois
     */
    private double calculateTimeInMonths(LocalDateTime launchDate) {
        if (launchDate == null) {
            return 0.0; // Si pas de date de lancement, considérer t=0
        }
        LocalDateTime now = LocalDateTime.now();
        return ChronoUnit.DAYS.between(launchDate, now) / 30.0; // Approximation : 30 jours = 1 mois
    }

    /**
     * Tarification par écrémage : P_écrémage(t) = P_max * e^(-α * t)
     * avec contrainte : P_écrémage(t) >= C * 1.2 (marge minimale de 20%)
     */
    public Mono<TarificationResult> calculateDecremageTarificationPrice(UUID productId, Double prixMax) {
        return produitService.getProductById(productId)
                .zipWith(authService.getUserFromContext())
                .flatMap(tuple -> {
                    Produit produit = tuple.getT1();
                    User currentUser = tuple.getT2();

                    // Calcul du temps écoulé depuis le lancement
                    double t = calculateTimeInMonths(produit.getDateLancement());

                    // Paramètres du modèle d'écrémage
                    double alpha = 0.03; // Taux de décroissance par mois
                    double prixMaximal = prixMax != null ? prixMax : produit.getPrixDesConcurrents() * 1.5;

                    // Formule d'écrémage
                    double tarificationPrice = prixMaximal * Math.exp(-alpha * t);

                    // Contrainte : prix minimum = coût de production + 20% de marge
                    double prixMinimal = produit.getCoutDeProduction() * 1.2;
                    if (tarificationPrice < prixMinimal) {
                        tarificationPrice = prixMinimal;
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
                    result.setTimeInMonths(t);

                    return tarificationRepository.save(result);
                });
    }

    /**
     * Tarification par pénétration : P_pénétration(t) = P_min + β * ln(t+1)
     */
    public Mono<TarificationResult> calculatePenetrationTarificationPrice(UUID productId, Double prixMin) {
        return produitService.getProductById(productId)
                .zipWith(authService.getUserFromContext())
                .flatMap(tuple -> {
                    Produit produit = tuple.getT1();
                    User currentUser = tuple.getT2();

                    // Calcul du temps écoulé depuis le lancement
                    double t = calculateTimeInMonths(produit.getDateLancement());

                    // Paramètres du modèle de pénétration
                    double prixMinimal = prixMin != null ? prixMin : produit.getCoutDeProduction() * 1.1;
                    double beta = 0.055 * produit.getCoutDeProduction(); // Taux d'augmentation

                    // Formule de pénétration
                    double tarificationPrice = prixMinimal + beta * Math.log(t + 1);

                    // Contrainte : ne pas descendre en dessous du prix minimal
                    if (tarificationPrice < prixMinimal) {
                        tarificationPrice = prixMinimal;
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
                    result.setTimeInMonths(t);

                    return tarificationRepository.save(result);
                });
    }

    /**
     * Tarification par alignement : P_alignement = moyenne_concurrents ± δ
     * δ peut varier légèrement dans le temps pour s'adapter au marché
     */
    public Mono<TarificationResult> calculateAlignementTarificationPrice(UUID productId) {
        return produitService.getProductById(productId)
                .zipWith(authService.getUserFromContext())
                .flatMap(tuple -> {
                    Produit produit = tuple.getT1();
                    User currentUser = tuple.getT2();

                    // Calcul du temps écoulé depuis le lancement
                    double t = calculateTimeInMonths(produit.getDateLancement());

                    // Prix des concurrents
                    Double prixConcurrents = produit.getPrixDesConcurrents();

                    // Ajustement dynamique basé sur le temps (optionnel)
                    // δ peut diminuer avec le temps pour se rapprocher des concurrents
                    double delta = 0.015 * prixConcurrents * Math.exp(-0.01 * t);
                    double tarificationPrice = prixConcurrents + delta;

                    // Contrainte : prix minimum = coût de production + 10% de marge
                    double prixMinimal = produit.getCoutDeProduction() * 1.1;
                    if (tarificationPrice < prixMinimal) {
                        tarificationPrice = prixMinimal;
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
                    result.setTimeInMonths(t);

                    return tarificationRepository.save(result);
                });
    }

    /**
     * Méthode pour calculer le prix à un moment donné dans le futur
     */
    public Mono<TarificationResult> calculateFuturePricing(UUID productId, String strategie,
                                                           int monthsInFuture, Double prixReference) {
        return produitService.getProductById(productId)
                .zipWith(authService.getUserFromContext())
                .flatMap(tuple -> {
                    Produit produit = tuple.getT1();
                    User currentUser = tuple.getT2();

                    // Temps futur
                    double t = calculateTimeInMonths(produit.getDateLancement()) + monthsInFuture;

                    double tarificationPrice;

                    switch (strategie.toUpperCase()) {
                        case "ECREMAGE":
                            double prixMax = prixReference != null ? prixReference : produit.getPrixDesConcurrents() * 1.5;
                            tarificationPrice = prixMax * Math.exp(-0.03 * t);
                            tarificationPrice = Math.max(tarificationPrice, produit.getCoutDeProduction() * 1.2);
                            break;

                        case "PENETRATION":
                            double prixMin = prixReference != null ? prixReference : produit.getCoutDeProduction() * 1.1;
                            double beta = 0.055 * produit.getCoutDeProduction();
                            tarificationPrice = prixMin + beta * Math.log(t + 1);
                            tarificationPrice = Math.max(tarificationPrice, prixMin);
                            break;

                        case "ALIGNEMENT":
                        default:
                            double delta = 0.015 * produit.getPrixDesConcurrents() * Math.exp(-0.01 * t);
                            tarificationPrice = produit.getPrixDesConcurrents() + delta;
                            tarificationPrice = Math.max(tarificationPrice, produit.getCoutDeProduction() * 1.1);
                            break;
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
                    result.setTimeInMonths(t);

                    return tarificationRepository.save(result);
                });
    }

    public Flux<TarificationResult> getPricingHistory() {
        return authService.getUserFromContext()
                .flatMapMany(user -> tarificationRepository.findByUserIdOrderByCalculatedAtDesc(user.getId()));
    }
}