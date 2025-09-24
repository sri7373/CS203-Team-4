package com.smu.tariff.auth;

import com.smu.tariff.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    @NotBlank @Size(min = 3, max = 64)
    public String username;

    @Email @NotBlank
    public String email;

    @NotBlank @Size(min = 6, max = 100)
    public String password;

    public Role role = Role.ANALYST;
}
