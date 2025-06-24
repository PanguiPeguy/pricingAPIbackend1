package com.ENSPY.Reseau.APIRest.exception;

import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        Map<String, Object> body = new HashMap<>();

        if (ex instanceof BadCredentialsException) {
            body.put("message", "Email ou mot de passe incorrect");
            return writeResponse(exchange, HttpStatus.UNAUTHORIZED, body);
        } else if (ex instanceof UsernameNotFoundException) {
            body.put("message", ex.getMessage());
            return writeResponse(exchange, HttpStatus.NOT_FOUND, body);
        } else if (ex instanceof AccessDeniedException) {
            body.put("message", "Vous n'avez pas la permission d'effectuer cette action");
            return writeResponse(exchange, HttpStatus.FORBIDDEN, body);
        } else if (ex instanceof RuntimeException) {
            body.put("message", ex.getMessage());
            return writeResponse(exchange, HttpStatus.BAD_REQUEST, body);
        } else {
            body.put("message", "Une erreur est survenue");
            return writeResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR, body);
        }
    }

    private Mono<Void> writeResponse(ServerWebExchange exchange, HttpStatus status, Map<String, Object> body) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body.toString().getBytes()))
        );
    }
}
