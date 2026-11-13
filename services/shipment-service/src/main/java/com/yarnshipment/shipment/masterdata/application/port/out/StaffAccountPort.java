package com.yarnshipment.shipment.masterdata.application.port.out;

import com.yarnshipment.shipment.masterdata.domain.model.StaffAccountSpec;
import com.yarnshipment.shipment.masterdata.domain.model.StaffRole;
import java.util.UUID;

/** Fabrika içi kullanıcı açar, rolünü ve adını değiştirir. */
public interface StaffAccountPort {

    /**
     * Kullanıcıyı geçici şifre özetiyle açar (ilk girişte şifresini değiştirmesi gerekir) ve kimliğini döner. Giriş adı
     * alınmışsa {@code LoginAccountExistsException} fırlatır.
     */
    UUID createStaff(StaffAccountSpec spec);

    /** Rolü değiştirir ve açık oturumları kapatır. Kullanıcı yoksa {@code LoginAccountNotFoundException} fırlatır. */
    void changeStaffRole(UUID userId, StaffRole role);

    /** Kullanıcının ad soyadını günceller; oturumlar, rol ve giriş adı değişmez. Kullanıcı yoksa not-found fırlatır. */
    void changeStaffName(UUID userId, String fullName);
}
