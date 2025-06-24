package com.ENSPY.Reseau.APIRest.config;

import com.ENSPY.Reseau.APIRest.controller.AuthController;
import com.ENSPY.Reseau.APIRest.controller.HomeController;
import com.ENSPY.Reseau.APIRest.controller.PricingController;
import com.ENSPY.Reseau.APIRest.controller.ProduitController;
import com.ENSPY.Reseau.APIRest.controller.TarificationController;
import com.ENSPY.Reseau.APIRest.model.Produit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.UUID;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterConfig {

    @Bean
    public RouterFunction<ServerResponse> routes(
            HomeController homeController,
            PricingController pricingController,
            TarificationController tarificationController,
            ProduitController produitController,
            AuthController authController) {
        return route()
                .GET("/", req -> homeController.home())
                .POST("/pricing/{productId}", req -> pricingController.calculateOptimalPrice(UUID.fromString(req.pathVariable("productId"))))
                .GET("/pricing/history", req -> pricingController.getPricingHistory())
                .POST("/tarification/ecremage/{prixMax}/{productId}", req -> tarificationController.calculateDecremageTarificationPrice(UUID.fromString(req.pathVariable("productId")), Integer.parseInt(req.pathVariable("prixMax"))))
                .POST("/tarification/alignement/{productId}", req -> tarificationController.calculateAlignementTarificationPrice(UUID.fromString(req.pathVariable("productId"))))
                .POST("/tarification/penetration/{prixMin}/{productId}", req -> tarificationController.calculatePenetrationTarificationPrice(UUID.fromString(req.pathVariable("productId")), Integer.parseInt(req.pathVariable("prixMin"))))
                .GET("/tarification/history", req -> tarificationController.getPricingHistory())
                .GET("/produit/read", rep -> produitController.getAllProducts())
                .GET("/produit/read/{id}", req -> produitController.getProductById(UUID.fromString(req.pathVariable("id"))))
                .POST("/produit/create", req -> req.bodyToMono(Produit.class).flatMap(produitController::createProduct))
                .PUT("/produit/update/{id}", req -> req.bodyToMono(Produit.class).flatMap(produit -> produitController.updateProduct(UUID.fromString(req.pathVariable("id")), produit)))
                .DELETE("/produit/delete/{id}", req -> produitController.deleteProduct(UUID.fromString(req.pathVariable("id"))))
                .POST("/auth/login", authController::login)
                .POST("/auth/register", authController::register)
                .PUT("/auth/update/{id}", req -> authController.updateUser(UUID.fromString(req.pathVariable("id")), req))
                .POST("/auth/upload-profile-picture/{id}", req -> authController.uploadProfilePicture(UUID.fromString(req.pathVariable("id")), req))
                .DELETE("/auth/delete/{id}", req -> authController.deleteUser(UUID.fromString(req.pathVariable("id"))))
                .build();
    }
}
