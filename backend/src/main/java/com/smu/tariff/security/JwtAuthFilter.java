package com.smu.tariff.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String username;

        if (authHeader == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.startsWith("Bearer ")
                ? authHeader.substring(7)
                : authHeader; // <-- allow raw token too

        try {
            username = jwtService.extractUsername(jwt);
        } catch (Exception e) {
            filterChain.doFilter(request, response);
            return;
        }

        // if (username != null &&
        // SecurityContextHolder.getContext().getAuthentication() == null) {
        // UserDetails userDetails =
        // this.userDetailsService.loadUserByUsername(username);
        // if (jwtService.isTokenValid(jwt, userDetails)) {
        // //debug
        // System.out.println("Auth header: " + authHeader);
        // System.out.println("Extracted username: " + username);

        // UsernamePasswordAuthenticationToken authToken = new
        // UsernamePasswordAuthenticationToken(userDetails,
        // null, userDetails.getAuthorities());
        // authToken.setDetails(new
        // WebAuthenticationDetailsSource().buildDetails(request));
        // SecurityContextHolder.getContext().setAuthentication(authToken);
        // }
        // }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            boolean valid = jwtService.isTokenValid(jwt, userDetails);
            System.out.println("Auth header: " + authHeader);
            System.out.println("Extracted username from token: " + username);
            System.out.println("UserDetails username: " + userDetails.getUsername());
            System.out.println("Token valid? " + valid);

            if (valid) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                System.out.println("Authentication set in context with authorities: " + userDetails.getAuthorities());
            } else {
                System.out.println("Token was invalid, skipping authentication.");
            }
        }

        filterChain.doFilter(request, response);
    }
}
