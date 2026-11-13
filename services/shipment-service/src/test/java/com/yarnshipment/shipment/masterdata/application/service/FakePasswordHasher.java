package com.yarnshipment.shipment.masterdata.application.service;

import com.yarnshipment.shipment.masterdata.application.port.out.PasswordHasherPort;

/** Testlerde {@link PasswordHasherPort} yerine geçer: özet {@code hash:<şifre>} biçimindedir. */
final class FakePasswordHasher implements PasswordHasherPort {

    private int spentVerifications;

    int spentVerifications() {
        return spentVerifications;
    }

    @Override
    public String hash(String password) {
        return "hash:" + password;
    }

    @Override
    public boolean matches(String password, String passwordHash) {
        return passwordHash.equals(hash(password));
    }

    @Override
    public void spendVerificationTime(String password) {
        spentVerifications++;
    }
}
