package com.yarnshipment.shipment.masterdata.application.service;

import com.yarnshipment.shipment.masterdata.application.port.out.AuthUserPort;
import com.yarnshipment.shipment.masterdata.domain.model.AuthUser;
import com.yarnshipment.shipment.masterdata.domain.model.StaffRole;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Testlerde {@link AuthUserPort} yerine geçen bellek içi hesap deposu; sayaç kuralı gerçek SQL ile aynıdır. */
final class FakeAuthUsers implements AuthUserPort {

    private final Map<UUID, AuthUser> users = new HashMap<>();
    private final Map<UUID, Integer> failures = new HashMap<>();

    /** Bu depoyu ve sahte özetleyiciyi kullanan şifre üretici. */
    PasswordIssuer issuer() {
        return new PasswordIssuer(this, new FakePasswordHasher());
    }

    /** Geçici şifreli, etkin bir hesap ekler; şifresi {@code hash:Sifre-12345} biçimindedir. */
    UUID add(String username) {
        return add(username, true, null);
    }

    UUID add(String username, boolean enabled, @Nullable Instant lockedUntil) {
        UUID id = UUID.randomUUID();
        users.put(
                id,
                new AuthUser(
                        id,
                        username,
                        "Test " + username,
                        StaffRole.WAREHOUSE,
                        "hash:Sifre-12345",
                        enabled,
                        true,
                        lockedUntil,
                        Set.of()));
        return id;
    }

    AuthUser user(UUID id) {
        return Objects.requireNonNull(users.get(id), "user");
    }

    int failures(UUID id) {
        return failures.getOrDefault(id, 0);
    }

    @Override
    public Optional<AuthUser> findByUsername(String username) {
        return users.values().stream()
                .filter(user -> user.username().toLowerCase(Locale.ROOT).equals(username.toLowerCase(Locale.ROOT)))
                .findFirst();
    }

    @Override
    public Optional<AuthUser> findById(UUID userId) {
        return Optional.ofNullable(users.get(userId));
    }

    @Override
    public void recordFailedAttempt(UUID userId, int maxAttempts, Duration lockFor, Instant now) {
        AuthUser user = user(userId);
        int count = failures.getOrDefault(userId, 0) + 1;
        if (count >= maxAttempts) {
            failures.put(userId, 0);
            users.put(userId, withLock(user, now.plus(lockFor)));
        } else {
            failures.put(userId, count);
        }
    }

    @Override
    public void recordSuccessfulLogin(UUID userId, Instant now) {
        failures.put(userId, 0);
        users.put(userId, withLock(user(userId), null));
    }

    @Override
    public void replacePasswordHash(UUID userId, String passwordHash, Instant now) {
        AuthUser user = user(userId);
        users.put(
                userId,
                new AuthUser(
                        user.id(),
                        user.username(),
                        user.fullName(),
                        user.role(),
                        passwordHash,
                        user.enabled(),
                        false,
                        null,
                        user.siteIds()));
    }

    private static AuthUser withLock(AuthUser user, @Nullable Instant lockedUntil) {
        return new AuthUser(
                user.id(),
                user.username(),
                user.fullName(),
                user.role(),
                user.passwordHash(),
                user.enabled(),
                user.passwordChangeRequired(),
                lockedUntil,
                user.siteIds());
    }
}
