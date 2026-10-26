package com.yarnshipment.shipment.masterdata.application.port.in;

import java.util.List;
import java.util.UUID;

/** Giriş hesabı açma isteği: giriş adı, isteğe bağlı e-posta ve görülecek lokasyonlar. */
public record LoginAccountRequest(String username, String email, List<UUID> siteIds) {

    public LoginAccountRequest {
        siteIds = List.copyOf(siteIds);
    }
}
