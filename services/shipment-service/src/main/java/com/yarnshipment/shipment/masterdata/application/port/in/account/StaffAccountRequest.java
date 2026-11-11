package com.yarnshipment.shipment.masterdata.application.port.in.account;

import com.yarnshipment.shipment.masterdata.domain.model.StaffRole;

/** Fabrika içi hesap açma isteği: giriş adı, ad soyad, rol ve yöneticinin belirlediği ilk şifre. */
public record StaffAccountRequest(String username, String fullName, StaffRole role, String password) {}
