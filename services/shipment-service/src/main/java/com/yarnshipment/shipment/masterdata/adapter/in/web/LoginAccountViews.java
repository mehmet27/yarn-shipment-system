package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.shipment.adapter.in.web.api.model.LoginAccountRequest;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Giriş hesabı isteği ve yanıtının web eşlemesi. */
final class LoginAccountViews {

    private LoginAccountViews() {}

    static com.yarnshipment.shipment.masterdata.application.port.in.LoginAccountRequest command(
            LoginAccountRequest request) {
        return new com.yarnshipment.shipment.masterdata.application.port.in.LoginAccountRequest(
                request.getUsername(), request.getPhone(), request.getEmail(), List.copyOf(request.getSiteIds()));
    }

    /** Şifre ya da kod dönmez; kod yalnızca kişinin telefonuna gider. */
    static ResponseEntity<Void> created() {
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .build();
    }
}
