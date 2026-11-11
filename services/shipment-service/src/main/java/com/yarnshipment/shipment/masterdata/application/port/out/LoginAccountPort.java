package com.yarnshipment.shipment.masterdata.application.port.out;

import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSummary;
import java.util.List;

/** Kimlik sağlayıcıda (Keycloak) uygulama rolü olan kullanıcıları listeler. */
public interface LoginAccountPort {

    /**
     * Uygulama rollerinden en az birine sahip kullanıcılar, giriş adına göre sıralı. Servis hesapları gelmez.
     * Sağlayıcı yanıt vermezse ya da yapılandırılmamışsa {@code LoginAccountUnavailableException} fırlatır.
     */
    List<LoginAccountSummary> findAll();
}
