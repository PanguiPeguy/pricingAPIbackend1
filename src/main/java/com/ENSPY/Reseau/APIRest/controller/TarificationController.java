package com.ENSPY.Reseau.APIRest.controller;

import com.ENSPY.Reseau.APIRest.model.TarificationResult;
import com.ENSPY.Reseau.APIRest.service.TarificationService;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@Controller
public class TarificationController {

    private final TarificationService tarificationService;

    public TarificationController(TarificationService tarificationService) {
        this.tarificationService = tarificationService;
    }

    public Mono<ServerResponse> calculateDecremageTarificationPrice(UUID productId, Integer prixMax) {
        return tarificationService.calculateDecremageTarificationPrice(productId, prixMax)
                .flatMap(result -> ServerResponse.ok().bodyValue(result))
                .onErrorResume(e -> ServerResponse.badRequest().bodyValue(Map.of("message", e.getMessage())));
    }

    public Mono<ServerResponse> calculateAlignementTarificationPrice(UUID productId) {
        return tarificationService.calculateAlignementTarificationPrice(productId)
                .flatMap(result -> ServerResponse.ok().bodyValue(result))
                .onErrorResume(e -> ServerResponse.badRequest().bodyValue(Map.of("message", e.getMessage())));
    }

    public Mono<ServerResponse> calculatePenetrationTarificationPrice(UUID productId, Integer prixMin) {
        return tarificationService.calculatePenetrationTarificationPrice(productId, prixMin)
                .flatMap(result -> ServerResponse.ok().bodyValue(result))
                .onErrorResume(e -> ServerResponse.badRequest().bodyValue(Map.of("message", e.getMessage())));
    }

    public Mono<ServerResponse> getPricingHistory() {
        return ServerResponse.ok().body(tarificationService.getPricingHistory(), TarificationResult.class);
    }
}
