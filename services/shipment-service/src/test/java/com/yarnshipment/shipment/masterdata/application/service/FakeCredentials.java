package com.yarnshipment.shipment.masterdata.application.service;

import com.yarnshipment.shipment.masterdata.application.port.out.AccountCredentialPort;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Testlerde {@link AccountCredentialPort} yerine geçen sahte uygulama; yapılan işlemleri kaydeder. */
final class FakeCredentials implements AccountCredentialPort {

    private final List<String> passwords = new ArrayList<>();
    private final List<String> enabledChanges = new ArrayList<>();

    List<String> passwords() {
        return passwords;
    }

    List<String> enabledChanges() {
        return enabledChanges;
    }

    @Override
    public void setTemporaryPasswordHash(UUID userId, String passwordHash) {
        passwords.add(userId + ":" + passwordHash);
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
