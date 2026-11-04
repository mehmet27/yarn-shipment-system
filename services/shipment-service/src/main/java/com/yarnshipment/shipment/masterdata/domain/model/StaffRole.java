package com.yarnshipment.shipment.masterdata.domain.model;

import java.util.Locale;

/**
 * Fabrika içi (ek kapsam kimliği gerektirmeyen) giriş rolleri. Şoför ve fasoncu rolleri bir kayda bağlı olduğu için
 * buradan verilmez ve değiştirilmez; onlar kendi kayıtlarından açılır.
 */
public enum StaffRole {
    ADMIN,
    WAREHOUSE,
    QUALITY,
    MANAGER;

    /** Keycloak realm rol adı, ör. {@code warehouse}. */
    public String realmRole() {
        return name().toLowerCase(Locale.ROOT);
    }
}
