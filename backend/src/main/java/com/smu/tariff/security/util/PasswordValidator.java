package com.smu.tariff.security.util;

import java.util.regex.Pattern;

public class PasswordValidator {
    
    /**
     * Regular expression pattern for password validation.
     * 
     * Pattern breakdown:
     * ^ - Start of string
     * (?=.*[0-9]) - Positive lookahead for at least one digit
     * (?=.*[a-z]) - Positive lookahead for at least one lowercase letter
     * (?=.*[A-Z]) - Positive lookahead for at least one uppercase letter
     * (?=.*[@#$%^&+=!]) - Positive lookahead for at least one special character
     * .{8,} - Match any character at least 8 times (minimum length)
     * $ - End of string
     */
    
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$");

    /**
     * Validates if a password meets the security requirements.
     * 
     * @param password the password string to validate
     * @return true if the password meets all requirements, false otherwise (including null passwords)
     */
    public static boolean isValid(String password) {
        // Check for null password to prevent NullPointerException
        if (password == null) {
            return false;
        }
        
        // Use compiled pattern to match against password
        return PASSWORD_PATTERN.matcher(password).matches();
    }
}
