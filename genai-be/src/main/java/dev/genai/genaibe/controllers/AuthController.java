package dev.genai.genaibe.controllers;

import dev.genai.genaibe.models.entities.User;
import dev.genai.genaibe.repositories.UserRepository;
import dev.genai.genaibe.services.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        // Αγνοούμε τον ρόλο στον έλεγχο για απλότητα, αρκεί να υπάρχει το email

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtService.generateToken(user);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "userId", user.getId(),
                "email", user.getEmail()
        ));
    }
}
