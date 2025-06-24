package com.ENSPY.Reseau.APIRest.repository;

import com.ENSPY.Reseau.APIRest.model.TarificationResult;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface TarificationRepository extends ReactiveCrudRepository<TarificationResult, UUID> {
    Flux<TarificationResult> findByUserIdOrderByCalculatedAtDesc(UUID userId);
    Flux<TarificationResult> findByProductId(UUID productId);
    Mono<Void> deleteByUserId(UUID userId);
}
