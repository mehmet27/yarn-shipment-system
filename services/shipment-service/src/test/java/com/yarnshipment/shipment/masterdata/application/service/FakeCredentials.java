package com.yarnshipment.shipment.masterdata.application.service;

import com.yarnshipment.shipment.masterdata.application.port.out.AccountCredentialPort;
import com.yarnshipment.shipment.masterdata.application.port.out.ResetTarget;
import com.yarnshipment.shipment.masterdata.domain.model.WeakPasswordException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Testlerde {@link AccountCredentialPort} yerine geçen sahte kimlik sağlayıcı; yapılan işlemleri kaydeder. */
final class FakeCredentials implements AccountCredentialPort {

    private final List<String> passwords = new ArrayList<>();
    private final List<String> enabledChanges = new ArrayList<>();
    private Optional<ResetTarget> target = Optional.empty();
    private boolean rejectPasswords;

    void target(Optional<ResetTarget> value) {
        this.target = value;
    }

    void rejectPasswords(boolean value) {
        this.rejectPasswords = value;
    }

    List<String> passwords() {
        return passwords;
    }

    List<String> enabledChanges() {
        return enabledChanges;
    }

    @Override
    public Optional<ResetTarget> findByUsername(String username) {
        return target.filter(found -> found.username().equals(username));
    }

    @Override
    public Optional<ResetTarget> findById(UUID userId) {
        return target.filter(found -> found.userId().equals(userId));
    }

    @Override
    public void setPassword(UUID userId, String password) {
        if (rejectPasswords) {
            throw new WeakPasswordException();
        }
        passwords.add(userId + ":" + password);
    }

    @Override
    public void enable(UUID userId) {
        enabledChanges.add(userId + ":true");
    }

    @Override
    public void disable(UUID userId) {
        enabledChanges.add(userId + ":false");
    }
}
