package com.smu.tariff.logging;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.smu.tariff.security.JwtService;
import com.smu.tariff.user.User;
import com.smu.tariff.user.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
@Transactional
public class QueryLogService {

    private final QueryLogRepository queryLogRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public QueryLogService(QueryLogRepository queryLogRepository,
                           UserRepository userRepository,
                           JwtService jwtService) {
        this.queryLogRepository = queryLogRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    /**
     * Save a query log and attach the currently authenticated user if available.
     */
    public void log(String type, String params) {
        User user = getCurrentUser();

        String resolvedUser = (user != null) ? user.getUsername() : "<anonymous>";
        System.out.println("QueryLogService: saving log for user=" + resolvedUser);
        queryLogRepository.save(new QueryLog(user, type, params));
    }

    public User getCurrentUser() {
        User user = resolveUserFromSecurityContext();
        if (user == null) {
            user = resolveUserFromRequest();
        }
        return user;
    }

    private User resolveUserFromSecurityContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            return userRepository.findByUsername(username).orElse(null);
        }

        String username = auth.getName();
        if (username != null && !"anonymousUser".equalsIgnoreCase(username)) {
            return userRepository.findByUsername(username).orElse(null);
        }
        return null;
    }

    private User resolveUserFromRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        if (request == null) {
            return null;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authHeader.substring(7);
        try {
            String username = jwtService.extractUsername(token);
            if (username != null && !username.isBlank()) {
                return userRepository.findByUsername(username).orElse(null);
            }
        } catch (Exception ex) {
            System.err.println("QueryLogService: failed to resolve user from token - " + ex.getMessage());
        }
        return null;
    }
}
