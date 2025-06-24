package com.ENSPY.Reseau.APIRest.controller;

import com.ENSPY.Reseau.APIRest.model.User;
import com.ENSPY.Reseau.APIRest.service.AuthService;
import com.ENSPY.Reseau.APIRest.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    public Mono<ServerResponse> login(ServerRequest request) {
        return request.bodyToMono(Map.class)
                .flatMap(loginRequest -> {
                    String email = (String) loginRequest.get("email");
                    String password = (String) loginRequest.get("password");
                    return authService.login(email, password)
                            .flatMap(response -> ServerResponse.ok().bodyValue(response))
                            .onErrorResume(e -> ServerResponse.status(HttpStatus.UNAUTHORIZED)
                                    .bodyValue(Map.of("message", e.getMessage())));
                });
    }

    public Mono<ServerResponse> register(ServerRequest request) {
        return request.bodyToMono(User.class)
                .flatMap(user -> {
                    user.setCreatedAt(LocalDateTime.now());
                    user.setUpdatedAt(LocalDateTime.now());
                    return authService.register(user)
                            .flatMap(newUser -> {
                                Map<String, Object> response = new HashMap<>();
                                response.put("id", newUser.getId());
                                response.put("email", newUser.getEmail());
                                response.put("firstName", newUser.getFirstName());
                                response.put("lastName", newUser.getLastName());
                                response.put("companyName", newUser.getCompanyName());
                                response.put("profilePicture", newUser.getProfilePicture());
                                return ServerResponse.status(HttpStatus.CREATED).bodyValue(response);
                            })
                            .onErrorResume(e -> ServerResponse.badRequest().bodyValue(Map.of("message", e.getMessage())));
                });
    }

    public Mono<ServerResponse> updateUser(UUID id, ServerRequest request) {
        return request.bodyToMono(User.class)
                .flatMap(user -> userService.updateUser(id, user)
                        .flatMap(updatedUser -> {
                            updatedUser.setUpdatedAt(LocalDateTime.now());
                            Map<String, Object> response = new HashMap<>();
                            response.put("id", updatedUser.getId());
                            response.put("email", updatedUser.getEmail());
                            response.put("firstName", updatedUser.getFirstName());
                            response.put("lastName", updatedUser.getLastName());
                            response.put("companyName", updatedUser.getCompanyName());
                            response.put("profilePicture", updatedUser.getProfilePicture());
                            return ServerResponse.ok().bodyValue(response);
                        })
                        .onErrorResume(e -> ServerResponse.badRequest().bodyValue(Map.of("message", e.getMessage()))));
    }

    public Mono<ServerResponse> uploadProfilePicture(UUID id, ServerRequest request) {
        return request.multipartData()
                .flatMap(multiPartData -> {
                    Part part = multiPartData.getFirst("file");
                    if (!(part instanceof FilePart filePart)) {
                        return ServerResponse.badRequest().bodyValue(Map.of("message", "Le fichier doit être un FilePart"));
                    }
                    return userService.uploadProfilePicture(id, filePart)
                            .flatMap(fileUrl -> userService.getUserById(id)
                                    .flatMap(updatedUser -> {
                                        Map<String, Object> response = new HashMap<>();
                                        response.put("profilePicture", fileUrl);
                                        response.put("user", Map.of(
                                                "id", updatedUser.getId(),
                                                "email", updatedUser.getEmail(),
                                                "firstName", updatedUser.getFirstName(),
                                                "lastName", updatedUser.getLastName(),
                                                "companyName", updatedUser.getCompanyName(),
                                                "profilePicture", updatedUser.getProfilePicture()
                                        ));
                                        return ServerResponse.ok().bodyValue(response);
                                    }))
                            .onErrorResume(IOException.class, e -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .bodyValue(Map.of("message", "Échec de l'upload: " + e.getMessage())))
                            .onErrorResume(e -> ServerResponse.badRequest().bodyValue(Map.of("message", e.getMessage())));
                });
    }

    public Mono<ServerResponse> deleteUser(UUID id) {
        return userService.deleteUser(id)
                .then(ServerResponse.ok().bodyValue(Map.of("message", "Utilisateur supprimé avec succès")))
                .onErrorResume(e -> ServerResponse.badRequest().bodyValue(Map.of("message", e.getMessage())));
    }
}