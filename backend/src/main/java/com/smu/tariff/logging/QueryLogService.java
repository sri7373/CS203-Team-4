package com.smu.tariff.logging;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smu.tariff.user.User;
import com.smu.tariff.user.UserRepository;

@Service
@Transactional
public class QueryLogService {

    private final QueryLogRepository queryLogRepository;
    private final UserRepository userRepository;

    public QueryLogService(QueryLogRepository queryLogRepository, UserRepository userRepository) {
        this.queryLogRepository = queryLogRepository;
        this.userRepository = userRepository;
    }

    /**
     * Save a query log and attach the currently authenticated user if available.
     */
    public void log(String type, String params) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = null;
        if (auth != null) {
            Object principal = auth.getPrincipal();
            if (principal instanceof User) {
                user = (User) principal;
            } else if (principal instanceof UserDetails) {
                String username = ((UserDetails) principal).getUsername();
                user = userRepository.findByUsername(username).orElse(null);
            }
        }

        System.out.println("QueryLogService: saving log for user=" + (user != null ? user.getUsername() : "<anonymous>"));
        queryLogRepository.save(new QueryLog(user, type, params));
    }
}
