package com.ENSPY.Reseau.APIRest.repository;

import com.ENSPY.Reseau.APIRest.model.OptimalPriceResult;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface OptimalPriceResultRepository extends ReactiveCrudRepository<OptimalPriceResult, UUID> {
    Flux<OptimalPriceResult> findByUserIdOrderByCalculatedAtDesc(UUID userId);
    Flux<OptimalPriceResult> findByProductId(UUID productId);
    Mono<Void> deleteByUserId(UUID userId);
}
