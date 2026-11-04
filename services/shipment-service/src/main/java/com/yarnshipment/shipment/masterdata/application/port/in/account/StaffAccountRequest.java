package com.yarnshipment.shipment.masterdata.application.port.in.account;

import com.yarnshipment.shipment.masterdata.domain.model.StaffRole;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Fabrika içi hesap açma isteği: giriş adı, telefon, isteğe bağlı e-posta, ad soyad, rol ve görülecek lokasyonlar. */
public record StaffAccountRequest(
        String username, String phone, @Nullable String email, String fullName, StaffRole role, List<UUID> siteIds) {

    public StaffAccountRequest {
        siteIds = List.copyOf(siteIds);
    }
}
