package com.ENSPY.Reseau.APIRest.controller;

import com.ENSPY.Reseau.APIRest.model.OptimalPriceResult;
import com.ENSPY.Reseau.APIRest.service.PricingService;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@Controller
public class PricingController {

    private final PricingService pricingService;

    public PricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    public Mono<ServerResponse> calculateOptimalPrice(UUID productId) {
        return pricingService.calculateOptimalPrice(productId)
                .flatMap(result -> ServerResponse.ok().bodyValue(result))
                .onErrorResume(e -> ServerResponse.badRequest().bodyValue(Map.of("message", e.getMessage())));
    }

    public Mono<ServerResponse> getPricingHistory() {
        return ServerResponse.ok().body(pricingService.getPricingHistory(), OptimalPriceResult.class);
    }
}
