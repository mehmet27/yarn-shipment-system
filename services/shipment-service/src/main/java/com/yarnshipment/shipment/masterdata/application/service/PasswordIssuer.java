package com.yarnshipment.shipment.masterdata.application.service;

import com.yarnshipment.shipment.masterdata.application.port.out.AuthUserPort;
import com.yarnshipment.shipment.masterdata.application.port.out.PasswordHasherPort;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountNotFoundException;
import com.yarnshipment.shipment.masterdata.domain.model.PasswordPolicy;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Yöneticinin yazdığı geçici şifreyi kurala göre denetler ve yalnızca özetini üretir. */
@Component
final class PasswordIssuer {

    private final AuthUserPort users;
    private final PasswordHasherPort hasher;

    PasswordIssuer(AuthUserPort users, PasswordHasherPort hasher) {
        this.users = users;
        this.hasher = hasher;
    }

    /** Yeni hesap için: şifre giriş adına göre denetlenir. */
    String hashForNewAccount(String username, String password) {
        PasswordPolicy.require(username, password);
        return hasher.hash(password);
    }

    /** Var olan hesap için; hesap yoksa {@link LoginAccountNotFoundException}. */
    String hashForAccount(UUID userId, String password) {
        String username = users.findById(userId)
                .orElseThrow(LoginAccountNotFoundException::new)
                .username();
        return hashForNewAccount(username, password);
    }
}
