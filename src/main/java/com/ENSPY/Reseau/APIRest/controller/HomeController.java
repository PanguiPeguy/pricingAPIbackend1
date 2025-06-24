package com.ENSPY.Reseau.APIRest.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Controller
public class HomeController {

    public Mono<ServerResponse> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Bienvenue sur l'API REST de ENSPY Reseau");
        response.put("status", "running");
        response.put("version", "1.0");

        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("login", "/auth/login");
        endpoints.put("register", "/auth/register");
        response.put("endpoints", endpoints);

        return ServerResponse.ok().bodyValue(response);
    }
}
