package com.yarnshipment.shipment.masterdata.domain.model;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    @Test
    @DisplayName("Yeterince uzun ve tahmin edilmesi zor şifre kabul edilir")
    void shouldAcceptGoodPassword() {
        assertThatCode(() -> PasswordPolicy.require("depo", "Gecici-Sifre-1")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Kısa, tek karakterli, giriş adıyla aynı ve sık kullanılan şifreler reddedilir")
    void shouldRejectWeakPasswords() {
        assertThatThrownBy(() -> PasswordPolicy.require("depo", "kisa123")).isInstanceOf(WeakPasswordException.class);
        assertThatThrownBy(() -> PasswordPolicy.require("depo", "aaaaaaaaaa"))
                .isInstanceOf(WeakPasswordException.class);
        assertThatThrownBy(() -> PasswordPolicy.require("depo.kullanici", "Depo.Kullanici"))
                .isInstanceOf(WeakPasswordException.class);
        assertThatThrownBy(() -> PasswordPolicy.require("depo", "12345678")).isInstanceOf(WeakPasswordException.class);
        assertThatThrownBy(() -> PasswordPolicy.require("depo", "Password123"))
                .isInstanceOf(WeakPasswordException.class);
        assertThatThrownBy(() -> PasswordPolicy.require("depo", "x".repeat(PasswordPolicy.MAX_LENGTH + 1)))
                .isInstanceOf(WeakPasswordException.class);
    }
}
