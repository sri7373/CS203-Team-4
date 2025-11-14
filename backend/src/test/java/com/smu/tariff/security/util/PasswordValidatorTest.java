package com.smu.tariff.security.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordValidatorTest {
    @Test
    void returnsFalseForShortPassword() {
        assertThat(PasswordValidator.isValid("A1!a")).isFalse();
    }

    @Test
    void returnsFalseForMissingDigit() {
        assertThat(PasswordValidator.isValid("InvalidAA!"))
            .isFalse();
    }

    @Test
    void returnsFalseForMissingSpecialChar() {
        assertThat(PasswordValidator.isValid("Valid1234")).isFalse();
    }

    @Test
    void returnsFalseForMissingLowercase() {
        assertThat(PasswordValidator.isValid("INVALID123!"))
            .isFalse();
    }

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
