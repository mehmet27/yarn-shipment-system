package com.yarnshipment.shipment.masterdata.domain.model;

/**
 * Fabrika içi (ek kapsam kimliği gerektirmeyen) giriş rolleri. Şoför ve fasoncu rolleri bir kayda bağlı olduğu için
 * buradan verilmez ve değiştirilmez; onlar kendi kayıtlarından açılır.
 */
public enum StaffRole {
    ADMIN,
    WAREHOUSE,
    QUALITY,
    MANAGER,
    GENERAL_MANAGER
}
