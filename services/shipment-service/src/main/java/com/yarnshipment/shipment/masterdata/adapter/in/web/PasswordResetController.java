package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.shipment.adapter.in.web.api.PasswordResetApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.PasswordResetConfirmRequest;
import com.yarnshipment.shipment.adapter.in.web.api.model.PasswordResetRequest;
import com.yarnshipment.shipment.masterdata.application.port.in.ConfirmPasswordResetUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.RequestPasswordResetUseCase;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * "Şifremi unuttum" uçları; girişsiz çağrılır (yollar {@code yarnshipment.security.public-paths} içindedir). Yanıt
 * giriş adının var olup olmadığını sızdırmaz. Yanıtlar önbelleğe alınmaz.
 * {@code permitAll()} açıkça yazılır: uç herkese açıktır ve yöntem güvenliği bu niyeti belgeler.
 */
@RestController
public class PasswordResetController implements PasswordResetApi {

    private final RequestPasswordResetUseCase requestReset;
    private final ConfirmPasswordResetUseCase confirmReset;

    public PasswordResetController(RequestPasswordResetUseCase requestReset, ConfirmPasswordResetUseCase confirmReset) {
        this.requestReset = requestReset;
        this.confirmReset = confirmReset;
    }

    @Override
    @PreAuthorize("permitAll()")
    public ResponseEntity<Void> requestPasswordReset(PasswordResetRequest request) {
        requestReset.requestPasswordReset(request.getUsername());
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).build();
    }

    @Override
    @PreAuthorize("permitAll()")
    public ResponseEntity<Void> confirmPasswordReset(PasswordResetConfirmRequest request) {
        confirmReset.confirmPasswordReset(request.getUsername(), request.getCode(), request.getNewPassword());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
