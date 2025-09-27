package com.smu.tariff.debug;

import com.smu.tariff.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/tariffs/debug")
public class DebugController {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public DebugController(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @GetMapping("/auth")
    public ResponseEntity<Map<String, Object>> authDebug(HttpServletRequest request) {
        Map<String, Object> out = new HashMap<>();
        try {
            String auth = request.getHeader("Authorization");
            out.put("authorizationHeader", auth);

            if (auth != null && auth.startsWith("Bearer ")) {
                String token = auth.substring(7);
                try {
                    String username = jwtService.extractUsername(token);
                    out.put("extractedUsername", username);
                    try {
                        UserDetails ud = userDetailsService.loadUserByUsername(username);
                        boolean valid = jwtService.isTokenValid(token, ud);
                        out.put("tokenValid", valid);
                    } catch (Exception e) {
                        out.put("userLookupError", e.getMessage());
                    }
                } catch (Exception e) {
                    out.put("tokenParseError", e.getMessage());
                }
            }

            Authentication authn = SecurityContextHolder.getContext().getAuthentication();
            out.put("securityAuthenticationPresent", authn != null);
            if (authn != null) {
                out.put("securityPrincipal", authn.getPrincipal() == null ? null : authn.getPrincipal().toString());
                out.put("securityAuthorities", authn.getAuthorities());
            }

            return ResponseEntity.ok(out);
        } catch (Exception e) {
            out.put("error", e.getMessage());
            return ResponseEntity.status(500).body(out);
        }
    }

    @GetMapping("/simple")
    public ResponseEntity<Map<String, Object>> simpleDebug(HttpServletRequest request) {
        Map<String, Object> out = new HashMap<>();
        try {
            String auth = request.getHeader("Authorization");
            out.put("authorizationHeader", auth);
            Authentication authn = SecurityContextHolder.getContext().getAuthentication();
            out.put("securityAuthenticationPresent", authn != null);
            if (authn != null) {
                out.put("securityPrincipalClass", authn.getPrincipal() == null ? null : authn.getPrincipal().getClass().getName());
                out.put("securityAuthorities", authn.getAuthorities());
            }
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            out.put("error", e.getMessage());
            return ResponseEntity.status(500).body(out);
        }
    }
}
