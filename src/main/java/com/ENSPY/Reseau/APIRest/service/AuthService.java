package com.ENSPY.Reseau.APIRest.service;

import com.ENSPY.Reseau.APIRest.config.JwtTokenProvider;
import com.ENSPY.Reseau.APIRest.model.User;
import com.ENSPY.Reseau.APIRest.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService implements ReactiveUserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private ReactiveAuthenticationManager authenticationManager;
    private JwtTokenProvider jwtTokenProvider;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired
    @Lazy
    public void setAuthenticationManager(ReactiveAuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Autowired
    @Lazy
    public void setJwtTokenProvider(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public Mono<Map<String, Object>> login(String email, String password) {
        return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password))
                .flatMap(authentication -> {
                    ReactiveSecurityContextHolder.getContext()
                            .contextWrite(ctx -> ctx.put(Authentication.class, authentication));
                    return userRepository.findByEmail(email)
                            .switchIfEmpty(Mono.error(new UsernameNotFoundException("User not found with email: " + email)))
                            .flatMap(user -> {
                                String token = jwtTokenProvider.createToken(user.getUsername(), String.valueOf(user.getId()));
                                Map<String, Object> userMap = new HashMap<>();
                                userMap.put("id", user.getId());
                                userMap.put("email", user.getEmail());
                                userMap.put("firstName", user.getFirstName());
                                userMap.put("lastName", user.getLastName());
                                userMap.put("companyName", user.getCompanyName());
                                userMap.put("profilePicture", user.getProfilePicture());

                                Map<String, Object> response = new HashMap<>();
                                response.put("token", token);
                                response.put("user", userMap);

                                return Mono.just(response);
                            });
                });
    }

    public Mono<User> register(User user) {
        return userRepository.existsByEmail(user.getEmail())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new RuntimeException("Email déjà utilisé"));
                    }
                    user.setPassword(passwordEncoder.encode(user.getPassword()));
                    return userRepository.save(user);
                });
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByEmail(username)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException("User not found with email: " + username)))
                .cast(UserDetails.class);
    }

    public Mono<User> getUserFromContext() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication().getName())
                .flatMap(email -> userRepository.findByEmail(email)
                        .switchIfEmpty(Mono.error(new UsernameNotFoundException("User not found with email: " + email))));
    }
}