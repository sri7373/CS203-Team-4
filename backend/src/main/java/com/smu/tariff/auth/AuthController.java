package com.smu.tariff.auth;

import com.smu.tariff.security.JwtService;
import com.smu.tariff.user.Role;
import com.smu.tariff.user.User;
import com.smu.tariff.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smu.tariff.security.JwtService;
import com.smu.tariff.security.util.PasswordValidator;
import com.smu.tariff.user.Role;
import com.smu.tariff.user.User;
import com.smu.tariff.user.UserRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username, request.password));
        User user = (User) auth.getPrincipal();
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getRole().name()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        // Bean Validation already checked blank or invalid fields
        String normalizedUsername = request.getUsername().trim();
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(normalizedUsername)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username is taken");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email is taken");
        }

        if (!PasswordValidator.isValid(request.getPassword())) {
            return ResponseEntity.badRequest().body(
                "Password must be at least 8 characters long (max 100), include uppercase, lowercase, a digit, and a special character."
            );
        }
        Role role = request.getRole() == null ? Role.USER : request.getRole();
        User user = new User(normalizedUsername, normalizedEmail,
                passwordEncoder.encode(request.getPassword()), role);
        userRepository.save(user);
        
        // Generate JWT token for the newly registered user
        String token = jwtService.generateToken(user);
        
        // Return JSON response with token, username, and role
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(token, user.getUsername(), user.getRole().name()));
    }
}


