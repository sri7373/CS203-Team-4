package com.smu.tariff.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.smu.tariff.security.JwtService;
import com.smu.tariff.user.Role;
import com.smu.tariff.user.User;
import com.smu.tariff.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.Authentication;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    JwtService jwtService;

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    AuthController authController;

    @Test
    void registerReturnsConflictIfUsernameTaken() {
        RegisterRequest request = buildRegisterRequest(" demoUser ", "demo@example.com", "Abcd1234!");
        when(userRepository.existsByUsername("demoUser")).thenReturn(true);

        ResponseEntity<?> response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo("Username is taken");
    }

    @Test
    void registerReturnsConflictIfEmailTaken() {
        RegisterRequest request = buildRegisterRequest("demoUser", "Demo@Example.com", "Abcd1234!");
        when(userRepository.existsByEmail("demo@example.com")).thenReturn(true);

        ResponseEntity<?> response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo("Email is taken");
    }

    @Test
    void registerRejectsWeakPassword() {
        RegisterRequest request = buildRegisterRequest("demoUser", "demo@example.com", "weakpass");

        ResponseEntity<?> response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).asString().contains("Password must be at least");
    }

    @Test
    void registerCreatesUserAndReturnsToken() {
        RegisterRequest request = buildRegisterRequest("demoUser", "demo@example.com", "Abcd1234!");
        when(passwordEncoder.encode("Abcd1234!")).thenReturn("hashed");
        when(jwtService.generateToken(any(User.class))).thenReturn("mock-token");

        ResponseEntity<?> response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(AuthResponse.class);
        AuthResponse authResponse = (AuthResponse) response.getBody();
        assertThat(authResponse.token).isEqualTo("mock-token");
        assertThat(authResponse.username).isEqualTo("demoUser");
        assertThat(authResponse.role).isEqualTo(Role.USER.name());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("hashed");
    }

    @Test
    void loginAuthenticatesAndReturnsJwt() {
        AuthRequest request = new AuthRequest();
        request.username = "alice";
        request.password = "Password123!";

        User principal = new User("alice", "alice@example.com", "hash", Role.ADMIN);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtService.generateToken(principal)).thenReturn("jwt-token");

        ResponseEntity<AuthResponse> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().token).isEqualTo("jwt-token");
        assertThat(response.getBody().username).isEqualTo("alice");
        assertThat(response.getBody().role).isEqualTo(Role.ADMIN.name());
    }

    private RegisterRequest buildRegisterRequest(String username, String email, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);
        request.setRole(Role.USER);
        return request;
    }
}
