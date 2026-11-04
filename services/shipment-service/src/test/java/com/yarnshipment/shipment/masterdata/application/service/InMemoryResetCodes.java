package com.yarnshipment.shipment.masterdata.application.service;

import com.yarnshipment.shipment.masterdata.application.port.out.ResetCode;
import com.yarnshipment.shipment.masterdata.application.port.out.ResetCodePort;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Testlerde {@link ResetCodePort} yerine geçen bellek içi depo. */
final class InMemoryResetCodes implements ResetCodePort {

    private final Map<UUID, ResetCode> stored = new HashMap<>();

    @Override
    public void save(ResetCode code) {
        stored.put(code.userId(), code);
    }

    @Override
    public Optional<ResetCode> find(UUID userId) {
        return Optional.ofNullable(stored.get(userId));
    }

    @Override
    public void recordFailedAttempt(UUID userId) {
        ResetCode code = get(userId);
        stored.put(
                userId,
                new ResetCode(
                        userId, code.codeHash(), code.salt(), code.issuedAt(), code.expiresAt(), code.attempts() + 1));
    }

    @Override
    public void delete(UUID userId) {
        stored.remove(userId);
    }

    boolean isEmpty() {
        return stored.isEmpty();
    }

    ResetCode get(UUID userId) {
        return find(userId).orElseThrow();
    }
}
