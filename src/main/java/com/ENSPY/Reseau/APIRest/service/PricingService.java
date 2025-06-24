package com.ENSPY.Reseau.APIRest.service;

import com.ENSPY.Reseau.APIRest.model.OptimalPriceResult;
import com.ENSPY.Reseau.APIRest.model.Produit;
import com.ENSPY.Reseau.APIRest.model.User;
import com.ENSPY.Reseau.APIRest.repository.OptimalPriceResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PricingService {

    private final OptimalPriceResultRepository optimalPriceResultRepository;
    private final ProduitService produitService;
    private final AuthService authService;
    private final WebClient webClient;

    public PricingService(
            OptimalPriceResultRepository optimalPriceResultRepository,
            ProduitService produitService,
            AuthService authService) {
        this.optimalPriceResultRepository = optimalPriceResultRepository;
        this.produitService = produitService;
        this.authService = authService;
        this.webClient = WebClient.builder()
                .baseUrl("https://pricingapibackend2.onrender.com")
                .build();
    }

    public Mono<OptimalPriceResult> calculateOptimalPrice(UUID productId) {
        return produitService.getProductById(productId)
                .zipWith(authService.getUserFromContext())
                .flatMap(tuple -> {
                    Produit produit = tuple.getT1();
                    User currentUser = tuple.getT2();

                    // Validation des données
                    if (produit.getCategory() == null || produit.getCategory().trim().isEmpty()) {
                        return Mono.error(new RuntimeException("La catégorie du produit est manquante"));
                    }
                    if (produit.getPrixDesConcurrents() == null || produit.getPrixDesConcurrents() <= 0) {
                        return Mono.error(new RuntimeException("Le prix des concurrents doit être positif"));
                    }
                    if (produit.getCoutDeProduction() == null || produit.getCoutDeProduction() <= 0) {
                        return Mono.error(new RuntimeException("Le coût de production doit être positif"));
                    }
                    if (produit.getDesiredMargin() == null || produit.getDesiredMargin() < 0) {
                        return Mono.error(new RuntimeException("La marge désirée doit être positive ou nulle"));
                    }

                    // Normaliser la marge en format décimal
                    double margeDecimale = produit.getDesiredMargin();
                    if (margeDecimale > 1.0) {
                        margeDecimale = margeDecimale / 100.0;
                    }

                    // Normaliser le domaine
                    double finalMargeDecimale = margeDecimale;
                    return normalizeCategory(produit.getCategory().trim())
                            .flatMap(normalizedCategory -> {
                                // Créer la requête pour l'API Flask
                                Map<String, Object> modelInput = new HashMap<>();
                                modelInput.put("domaine", normalizedCategory);
                                modelInput.put("prix_concurrent", produit.getPrixDesConcurrents().doubleValue());
                                modelInput.put("cout_production", produit.getCoutDeProduction().doubleValue());
                                modelInput.put("marge_voulue", finalMargeDecimale);

                                // Loguer la requête envoyée
                                System.out.println("=== DONNÉES ENVOYÉES AU SERVICE ML ===");
                                System.out.println("domaine: " + modelInput.get("domaine"));
                                System.out.println("prix_concurrent: " + modelInput.get("prix_concurrent"));
                                System.out.println("cout_production: " + modelInput.get("cout_production"));
                                System.out.println("marge_voulue: " + modelInput.get("marge_voulue"));
                                System.out.println("=====================================");

                                return callMLPredictionService(modelInput)
                                        .onErrorResume(e -> {
                                            System.err.println("Erreur lors de l'appel au service ML: " + e.getMessage());
                                            double fallbackPrice = produit.getCoutDeProduction() * (1 + finalMargeDecimale);
                                            System.err.println("Utilisation du prix de secours: " + fallbackPrice);
                                            return Mono.just(fallbackPrice);
                                        })
                                        .flatMap(optimalPrice -> {
                                            double potentialRevenue = optimalPrice * (produit.getStock() != null ? produit.getStock() : 0);
                                            double margin = ((optimalPrice - produit.getCoutDeProduction()) / optimalPrice) * 100;

                                            OptimalPriceResult result = new OptimalPriceResult();
                                            result.setProductId(produit.getId());
                                            result.setProductName(produit.getName());
                                            result.setPrixDesConcurrents(produit.getPrixDesConcurrents());
                                            result.setOptimalPrice(optimalPrice);
                                            result.setPotentialRevenue(potentialRevenue);
                                            result.setMargin(margin);
                                            result.setUserId(currentUser.getId());

                                            return optimalPriceResultRepository.save(result);
                                        });
                            });
                });
    }

    private Mono<String> normalizeCategory(String category) {
        return webClient.get()
                .uri("/domains")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Map<String, Object> domains = (Map<String, Object>) response.get("domaines_disponibles");
                    for (String domain : domains.keySet()) {
                        if (category.equalsIgnoreCase(domain)) {
                            return domain;
                        }
                    }
                    throw new RuntimeException("Domaine non reconnu: " + category + ". Domaines disponibles: " + domains.keySet());
                })
                .onErrorResume(e -> Mono.error(new RuntimeException("Erreur lors de la normalisation du domaine: " + e.getMessage())));
    }

    private Mono<Double> callMLPredictionService(Map<String, Object> modelInput) {
        return webClient.post()
                .uri("/predict")
                .header("Content-Type", "application/json")
                .bodyValue(modelInput)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new RuntimeException("Erreur 4xx: " + errorBody)))
                )
                .onStatus(
                        status -> status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new RuntimeException("Erreur 5xx: " + errorBody)))
                )
                .bodyToMono(MLPredictionResponse.class)
                .flatMap(response -> {
                    System.out.println("=== RÉPONSE DE L'API FLASK ===");
                    System.out.println("Statut: " + response.getStatut());
                    System.out.println("Prix prédit: " + response.getPrixPredit());
                    System.out.println("=============================");
                    if ("success".equals(response.getStatut())) {
                        double predictedPrice = response.getPrixPredit();
                        if (predictedPrice <= 0) {
                            return Mono.error(new RuntimeException("Prix prédit invalide: " + predictedPrice));
                        }
                        return Mono.just(predictedPrice);
                    }
                    return Mono.error(new RuntimeException("La prédiction ML a échoué avec le statut: " + response.getStatut()));
                });
    }

    public Flux<OptimalPriceResult> getPricingHistory() {
        return authService.getUserFromContext()
                .flatMapMany(user -> optimalPriceResultRepository.findByUserIdOrderByCalculatedAtDesc(user.getId()));
    }

    private static class MLPredictionResponse {
        private double prixPredit;
        private CaracteristiquesUtilisees caracteristiquesUtilisees;
        private String statut;

        public double getPrixPredit() {
            return prixPredit;
        }

        public void setPrixPredit(double prixPredit) {
            this.prixPredit = prixPredit;
        }

        public CaracteristiquesUtilisees getCaracteristiquesUtilisees() {
            return caracteristiquesUtilisees;
        }

        public void setCaracteristiquesUtilisees(CaracteristiquesUtilisees caracteristiquesUtilisees) {
            this.caracteristiquesUtilisees = caracteristiquesUtilisees;
        }

        public String getStatut() {
            return statut;
        }

        public void setStatut(String statut) {
            this.statut = statut;
        }
    }

    private static class CaracteristiquesUtilisees {
        private Object domaine;
        private int domaineEncode;
        private double prixConcurrent;
        private double coutProduction;
        private double margeVoulue;

        public Object getDomaine() {
            return domaine;
        }

        public void setDomaine(Object domaine) {
            this.domaine = domaine;
        }

        public int getDomaineEncode() {
            return domaineEncode;
        }

        public void setDomaineEncode(int domaineEncode) {
            this.domaineEncode = domaineEncode;
        }

        public double getPrixConcurrent() {
            return prixConcurrent;
        }

        public void setPrixConcurrent(double prixConcurrent) {
            this.prixConcurrent = prixConcurrent;
        }

        public double getCoutProduction() {
            return coutProduction;
        }

        public void setCoutProduction(double coutProduction) {
            this.coutProduction = coutProduction;
        }

        public double getMargeVoulue() {
            return margeVoulue;
        }

        public void setMargeVoulue(double margeVoulue) {
            this.margeVoulue = margeVoulue;
        }
    }
}