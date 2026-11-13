package com.yarnshipment.shipment.masterdata.application.port.in.auth;

import com.yarnshipment.shipment.masterdata.domain.model.SessionUser;

/** Kullanıcı adı ve şifreyi doğrular. */
public interface AuthenticateUseCase {

    /**
     * Doğrulanan kullanıcıyı döner. Kullanıcı adı ya da şifre yanlışsa ya da hesap kapalıysa
     * {@code AuthenticationFailedException}; hesap kilitliyse {@code AccountLockedException} fırlatır.
     */
    SessionUser authenticate(String username, String password);
}
