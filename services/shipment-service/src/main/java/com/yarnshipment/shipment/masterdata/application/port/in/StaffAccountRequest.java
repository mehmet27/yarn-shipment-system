package com.yarnshipment.shipment.masterdata.application.port.in;

import com.yarnshipment.shipment.masterdata.domain.model.StaffRole;
import java.util.List;
import java.util.UUID;

/** Fabrika içi hesap açma isteği: giriş adı, e-posta, ad soyad, rol ve görülecek lokasyonlar. */
public record StaffAccountRequest(String username, String email, String fullName, StaffRole role, List<UUID> siteIds) {

    public StaffAccountRequest {
        siteIds = List.copyOf(siteIds);
    }
}
