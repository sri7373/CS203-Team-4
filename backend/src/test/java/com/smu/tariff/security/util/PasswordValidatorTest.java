package com.smu.tariff.security.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordValidatorTest {

    @Test
    void returnsTrueForValidPassword() {
        assertThat(PasswordValidator.isValid("Valid123!")).isTrue();
    }

    @Test
    void returnsFalseForMissingUppercase() {
        assertThat(PasswordValidator.isValid("invalid123!")).isFalse();
    }

    @Test
    void returnsFalseForNullInput() {
        assertThat(PasswordValidator.isValid(null)).isFalse();
    }
}
