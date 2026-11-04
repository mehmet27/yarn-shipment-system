package com.yarnshipment.shipment.masterdata.application.port.out;

import com.yarnshipment.shipment.masterdata.domain.model.StaffAccountSpec;
import com.yarnshipment.shipment.masterdata.domain.model.StaffRole;
import java.util.UUID;

/** Kimlik sağlayıcıda (Keycloak) fabrika içi kullanıcı açar ve rolünü değiştirir. */
public interface StaffAccountPort {

    /**
     * Kullanıcıyı şifresiz açar, rolünü verir ve şifre belirleme e-postasını gönderir. Herhangi bir adım başarısız
     * olursa kullanıcı silinir. Giriş adı ya da e-posta alınmışsa {@code LoginAccountExistsException}; sağlayıcı (ya da
     * e-posta) yanıt vermezse {@code LoginAccountUnavailableException} fırlatır.
     */
    void createStaff(StaffAccountSpec spec);

    /**
     * Kullanıcının fabrika içi rolünü değiştirir ve açık oturumlarını kapatır. Kullanıcı yoksa
     * {@code LoginAccountNotFoundException}; şoför/fasoncu hesabıysa {@code LoginAccountRoleLockedException}.
     */
    void changeStaffRole(UUID userId, StaffRole role);
}
