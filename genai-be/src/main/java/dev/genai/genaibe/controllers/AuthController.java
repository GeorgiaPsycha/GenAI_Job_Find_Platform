package dev.genai.genaibe.controllers;

import dev.genai.genaibe.models.entities.User;
import dev.genai.genaibe.repositories.UserRepository;
import dev.genai.genaibe.services.JwtService;
import org.springframework.http.HttpStatus;
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

    // Check if the email exists as a person in the DB
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String requestedRole = request.get("role");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String actualRole = (user.getRole() != null) ? user.getRole() : "user";

        if (!actualRole.equalsIgnoreCase(requestedRole)) {
            // return error if the person is trying to enter "wrong" role
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access Denied: Incorrect role selected for this email."));
        }
        String token = jwtService.generateToken(user);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "userId", user.getId(),
                "email", user.getEmail(),
                "role", actualRole
        ));
    }
}
