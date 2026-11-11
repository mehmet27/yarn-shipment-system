package com.yarnshipment.shipment.masterdata.application.port.in.account;

import com.yarnshipment.shipment.masterdata.domain.model.StaffRole;
import com.yarnshipment.shipment.masterdata.domain.model.StaffWorkArea;
import org.jspecify.annotations.Nullable;

/**
 * Fabrika içi hesap açma isteği: giriş adı, ad soyad, rol ve yöneticinin belirlediği ilk şifre. Kalite rolünde
 * çalışma alanı (merkez / fasoncu) zorunludur; diğer rollerde verilmez.
 */
public record StaffAccountRequest(
        String username,
        String fullName,
        StaffRole role,
        String password,
        @Nullable StaffWorkArea workArea) {

    public StaffAccountRequest(String username, String fullName, StaffRole role, String password) {
        this(username, fullName, role, password, null);
    }
}
