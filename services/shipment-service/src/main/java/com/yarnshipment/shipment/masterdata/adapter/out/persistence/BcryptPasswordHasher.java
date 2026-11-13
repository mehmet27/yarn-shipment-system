package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import com.yarnshipment.shipment.masterdata.application.port.out.PasswordHasherPort;
import java.util.Map;
import java.util.Objects;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * {@link PasswordHasherPort}'un Spring Security uygulaması: BCrypt (maliyet 12), özet {@code {bcrypt}} önekiyle
 * saklanır; ileride algoritma değişirse eski özetler çalışmaya devam eder.
 */
@Component
final class BcryptPasswordHasher implements PasswordHasherPort {

    private static final int STRENGTH = 12;
    private static final String ID = "bcrypt";

    private static final String PREFIX = "{" + ID + "}";

    private final PasswordEncoder encoder =
            new DelegatingPasswordEncoder(ID, Map.of(ID, new BCryptPasswordEncoder(STRENGTH)));
    private final String dummyHash = hash("dummy-password-for-timing");

    @Override
    public String hash(String password) {
        return Objects.requireNonNull(encoder.encode(password), "hash");
    }

    @Override
    public boolean matches(String password, String passwordHash) {
        if (!passwordHash.startsWith(PREFIX)) {
            // Şifresi henüz belirlenmemiş hesap ("!"): giriş kapalıdır, süre yine de harcanır.
            spendVerificationTime(password);
            return false;
        }
        return encoder.matches(password, passwordHash);
    }

    @Override
    public void spendVerificationTime(String password) {
        encoder.matches(password, dummyHash);
    }
}
