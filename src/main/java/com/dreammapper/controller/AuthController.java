package com.dreammapper.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dreammapper.model.User;
import com.dreammapper.repository.UserRepository;
import com.dreammapper.service.impl.JwtService;

import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, max = 100) String password
    ) {}
    public record TokenResponse(String token) {}

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody LoginRequest req) {
        if (userRepository.findByEmail(req.email()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already in use"));
        }
        User u = User.builder()
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .name(req.email())
                .build();
        userRepository.save(u);
        String token = jwtService.generateToken(u.getEmail(), Map.of("uid", u.getId()));
        return ResponseEntity.ok(new TokenResponse(token));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        var u = userRepository.findByEmail(req.email())
                .orElse(null);
        if (u == null || !passwordEncoder.matches(req.password(), u.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
        String token = jwtService.generateToken(u.getEmail(), Map.of("uid", u.getId()));
        return ResponseEntity.ok(new TokenResponse(token));
    }
}
