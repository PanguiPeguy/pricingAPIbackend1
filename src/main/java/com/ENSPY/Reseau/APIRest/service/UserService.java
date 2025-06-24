package com.ENSPY.Reseau.APIRest.service;

import com.ENSPY.Reseau.APIRest.model.User;
import com.ENSPY.Reseau.APIRest.repository.OptimalPriceResultRepository;
import com.ENSPY.Reseau.APIRest.repository.ProduitRepository;
import com.ENSPY.Reseau.APIRest.repository.TarificationRepository;
import com.ENSPY.Reseau.APIRest.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ProduitRepository produitRepository;
    private final TarificationRepository tarificationRepository;
    private final OptimalPriceResultRepository optimalPriceResultRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public UserService(UserRepository userRepository, ProduitRepository produitRepository,
                       TarificationRepository tarificationRepository,
                       OptimalPriceResultRepository optimalPriceResultRepository,
                       PasswordEncoder passwordEncoder, AuthService authService) {
        this.userRepository = userRepository;
        this.produitRepository = produitRepository;
        this.tarificationRepository = tarificationRepository;
        this.optimalPriceResultRepository = optimalPriceResultRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    public Mono<User> getUserById(UUID id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Utilisateur non trouvé")));
    }

    public Mono<User> updateUser(UUID id, User userDetails) {
        return authService.getUserFromContext()
                .flatMap(currentUser -> {
                    if (!currentUser.getId().equals(id)) {
                        return Mono.error(new RuntimeException("Vous n'êtes pas autorisé à modifier cet utilisateur"));
                    }
                    return getUserById(id)
                            .map(user -> {
                                if (userDetails.getFirstName() != null) {
                                    user.setFirstName(userDetails.getFirstName());
                                }
                                if (userDetails.getLastName() != null) {
                                    user.setLastName(userDetails.getLastName());
                                }
                                if (userDetails.getCompanyName() != null) {
                                    user.setCompanyName(userDetails.getCompanyName());
                                }
                                if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
                                    user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
                                }
                                user.setUpdatedAt(LocalDateTime.now());
                                return user;
                            })
                            .flatMap(userRepository::save);
                });
    }

    public Mono<String> uploadProfilePicture(UUID userId, FilePart filePart) {
        return getUserById(userId)
                .flatMap(user -> authService.getUserFromContext()
                        .switchIfEmpty(Mono.just(user))
                        .flatMap(currentUser -> {
                            if (currentUser != null && !currentUser.getId().equals(userId)) {
                                return Mono.error(new RuntimeException("Vous n'êtes pas autorisé à modifier cet utilisateur"));
                            }
                            if (filePart == null) {
                                return Mono.error(new IllegalArgumentException("Le fichier est vide"));
                            }
                            String contentType = filePart.headers().getContentType().toString();
                            if (!contentType.startsWith("image/")) {
                                return Mono.error(new IllegalArgumentException("Le fichier doit être une image"));
                            }
                            String fileName = UUID.randomUUID().toString() + getFileExtension(filePart.filename());
                            Path uploadPath = Paths.get(uploadDir);
                            Path targetLocation = uploadPath.resolve(fileName);

                            return Mono.fromCallable(() -> Files.createDirectories(uploadPath))
                                    .then(filePart.transferTo(targetLocation))
                                    .then(Mono.just(fileName))
                                    .flatMap(fileNameResult -> {
                                        String fileUrl = "/uploads/profile-pictures/" + fileNameResult;
                                        user.setProfilePicture(fileUrl);
                                        user.setUpdatedAt(LocalDateTime.now());
                                        return userRepository.save(user)
                                                .thenReturn(fileUrl);
                                    });
                        }));
    }

    public Mono<Void> deleteUser(UUID id) {
        return authService.getUserFromContext()
                .flatMap(currentUser -> {
                    if (!currentUser.getId().equals(id)) {
                        return Mono.error(new RuntimeException("Vous n'êtes pas autorisé à supprimer cet utilisateur"));
                    }
                    return getUserById(id)
                            .flatMap(user -> {
                                Mono<Void> deleteFile = user.getProfilePicture() != null
                                        ? Mono.fromCallable(() -> {
                                    String fileName = user.getProfilePicture().substring(user.getProfilePicture().lastIndexOf("/") + 1);
                                    Path filePath = Paths.get(uploadDir).resolve(fileName);
                                    Files.deleteIfExists(filePath);
                                    return null;
                                })
                                        : Mono.empty();
                                // Supprimer d'abord les dépendances dans optimal_prices et tarification_prices
                                Mono<Void> deleteOptimalPrices = optimalPriceResultRepository.deleteByUserId(id);
                                Mono<Void> deleteTarificationPrices = tarificationRepository.deleteByUserId(id);
                                // Puis supprimer les produits
                                Mono<Void> deleteProduits = produitRepository.deleteByUserId(id);
                                // Puis supprimer l'utilisateur
                                return deleteOptimalPrices
                                        .then(deleteTarificationPrices)
                                        .then(deleteProduits)
                                        .then(deleteFile)
                                        .then(userRepository.delete(user));
                            });
                });
    }

    private String getFileExtension(String filename) {
        return filename.contains(".") ? filename.substring(filename.lastIndexOf(".")) : "";
    }
}
