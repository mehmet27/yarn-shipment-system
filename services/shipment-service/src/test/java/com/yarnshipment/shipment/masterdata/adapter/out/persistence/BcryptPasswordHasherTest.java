package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.yarnshipment.shipment.masterdata.domain.model.PasswordPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BcryptPasswordHasherTest {

    private final BcryptPasswordHasher hasher = new BcryptPasswordHasher();

    @Test
    @DisplayName("Şifre kuralının kabul ettiği en uzun şifre özetlenir ve doğrulanır")
    void shouldHashLongestAllowedPassword() {
        String longest = "Abc-12345".repeat(PasswordPolicy.MAX_BYTES / "Abc-12345".length());
        assertThatCode(() -> PasswordPolicy.require("depo", longest)).doesNotThrowAnyException();

        String hash = hasher.hash(longest);

        assertThat(hash).startsWith("{bcrypt}").doesNotContain(longest);
        assertThat(hasher.matches(longest, hash)).isTrue();
        assertThat(hasher.matches(longest.substring(1), hash)).isFalse();
    }

    @Test
    @DisplayName("Şifresi belirlenmemiş hesabın özeti hiçbir şifreyle eşleşmez")
    void shouldNeverMatchUnusableHash() {
        assertThat(hasher.matches("Gecici-Sifre-1", "!")).isFalse();
    }
}
