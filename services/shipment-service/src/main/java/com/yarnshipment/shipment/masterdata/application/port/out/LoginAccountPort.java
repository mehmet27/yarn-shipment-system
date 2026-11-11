package com.yarnshipment.shipment.masterdata.application.port.out;

import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSpec;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSummary;
import java.util.List;
import java.util.UUID;

/** Kimlik sağlayıcıda (Keycloak) kullanıcı açar ve uygulama rolü olan kullanıcıları listeler. */
public interface LoginAccountPort {

    /**
     * Kullanıcıyı şifresiz; rol, telefon ve kimlik/lokasyon öznitelikleriyle açar ve kimliğini döner. Kayıt için
     * hesap zaten varsa ya da giriş adı alınmışsa {@code LoginAccountExistsException}; sağlayıcı yanıt vermezse
     * {@code LoginAccountUnavailableException} fırlatır. Yarım kalan hesap bırakılmaz.
     */
    UUID create(LoginAccountSpec spec);

    /**
     * Uygulama rollerinden en az birine sahip kullanıcılar, giriş adına göre sıralı. Servis hesapları gelmez.
     * Sağlayıcı yanıt vermezse ya da yapılandırılmamışsa {@code LoginAccountUnavailableException} fırlatır.
     */
    List<LoginAccountSummary> findAll();
}
