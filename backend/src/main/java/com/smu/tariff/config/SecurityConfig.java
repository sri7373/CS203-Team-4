package com.smu.tariff.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity; 
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.smu.tariff.security.JwtAuthFilter;
import com.smu.tariff.security.JwtService;
import com.smu.tariff.user.UserRepository;

@EnableWebSecurity   // enables Spring Security filters
@EnableMethodSecurity // allow @PreAuthorize, @Secured, etc.
@Configuration
public class SecurityConfig {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public SecurityConfig(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    // spring Security automatically detects and wires  UserDetailsService
    @Bean
    public UserDetailsService userDetailsService() {
        // Loads a user from the database by username
        return username -> userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    // defines the password encoder (BCrypt)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // connects UserDetailsService + PasswordEncoder
    // Spring uses this provider to authenticate login credentials
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // provides AuthenticationManager bean (used in AuthController login)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // main security configuration 
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())

            // enables CORS with default configuration (cross-origin requests)
            .cors(Customizer.withDefaults())

            // stateless session — JWT used instead of HTTP session cookies
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // equivalent to slide’s authorizeHttpRequests()
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(
                            "/",
                            "/api/auth/**",
                            "/api/trade/**",
                            "/v3/api-docs/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/actuator/**"
                    ).permitAll()
                    .anyRequest().authenticated()
            )

            .httpBasic(Customizer.withDefaults())

            // registers our custom AuthenticationProvider
            .authenticationProvider(authenticationProvider())

            // adds JWT filter before the built-in UsernamePasswordAuthenticationFilter
            // This ensures JWT tokens are validated before any authentication step
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // builds and returns the configured filter chain
        return http.build();
    }
}

