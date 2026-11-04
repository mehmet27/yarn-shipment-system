package com.yarnshipment.shipment.masterdata.application.port.out;

import com.yarnshipment.shipment.masterdata.domain.model.StaffAccountSpec;
import com.yarnshipment.shipment.masterdata.domain.model.StaffRole;
import java.util.UUID;

/** Kimlik sağlayıcıda (Keycloak) fabrika içi kullanıcı açar ve rolünü değiştirir. */
public interface StaffAccountPort {

    /**
     * Kullanıcıyı şifresiz açar, rolünü ve telefonunu yazar, kimliğini döner. Rol verilemezse kullanıcı silinir.
     * Giriş adı ya da e-posta alınmışsa {@code LoginAccountExistsException}; sağlayıcı yanıt vermezse
     * {@code LoginAccountUnavailableException} fırlatır.
     */
    UUID createStaff(StaffAccountSpec spec);

    /**
     * Kullanıcının fabrika içi rolünü değiştirir ve açık oturumlarını kapatır. Kullanıcı yoksa
     * {@code LoginAccountNotFoundException}; şoför/fasoncu hesabıysa {@code LoginAccountRoleLockedException}.
     */
    void changeStaffRole(UUID userId, StaffRole role);
}
