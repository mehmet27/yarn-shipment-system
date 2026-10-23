package com.yarnshipment.shipment.masterdata.application.port.out;

import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSpec;

/** Kimlik sağlayıcıda (Keycloak) kullanıcı açar. */
public interface LoginAccountPort {

    /**
     * Kullanıcıyı geçici şifre, rol ve kimlik/lokasyon öznitelikleriyle açar. Kayıt için hesap zaten varsa ya da
     * giriş adı alınmışsa {@code LoginAccountExistsException}; sağlayıcı yanıt vermezse
     * {@code LoginAccountUnavailableException} fırlatır. Yarım kalan hesap bırakılmaz.
     */
    void create(LoginAccountSpec spec);
}
