package com.smu.tariff.auth;

import jakarta.validation.constraints.NotBlank;

public class AuthRequest {
    @NotBlank(message = "Username cannot be blank")
    public String username;
    
    @NotBlank(message = "Password cannot be blank")
    public String password;
}
