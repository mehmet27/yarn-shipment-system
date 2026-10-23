package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.shipment.adapter.in.web.api.model.LoginAccountRequest;
import com.yarnshipment.shipment.adapter.in.web.api.model.LoginAccountResponse;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccount;
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
                request.getUsername(), request.getEmail(), List.copyOf(request.getSiteIds()));
    }

    /** Geçici şifre içerdiği için yanıt önbelleğe alınmaz. */
    static ResponseEntity<LoginAccountResponse> created(LoginAccount account) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(new LoginAccountResponse(account.username(), account.temporaryPassword()));
    }
}
