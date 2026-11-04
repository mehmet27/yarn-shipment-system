package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.api.LoginAccountAccessApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.ChangeLoginAccountEnabledRequest;
import com.yarnshipment.shipment.masterdata.application.port.in.ChangeLoginAccountEnabledUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.StartPasswordResetUseCase;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * Giriş hesabının erişimi: pasife alma, yeniden açma ve yönetici başlatmalı şifre sıfırlama; yalnızca USER_MANAGE
 * yetkisi olan yönetici. Şifreyi yönetici görmez, kişi SMS koduyla kendisi belirler. Method security proxy'si
 * gerektirdiği için final değildir (SEC-02).
 */
@RestController
public class LoginAccountAccessController implements LoginAccountAccessApi {

    private final ChangeLoginAccountEnabledUseCase changeEnabled;
    private final StartPasswordResetUseCase startPasswordReset;
    private final CurrentActorProvider currentActorProvider;

    public LoginAccountAccessController(
            ChangeLoginAccountEnabledUseCase changeEnabled,
            StartPasswordResetUseCase startPasswordReset,
            CurrentActorProvider currentActorProvider) {
        this.changeEnabled = changeEnabled;
        this.startPasswordReset = startPasswordReset;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<Void> changeLoginAccountEnabled(UUID userId, ChangeLoginAccountEnabledRequest request) {
        if (request.getEnabled()) {
            changeEnabled.enableLoginAccount(currentActorProvider.getCurrentActor(), userId);
        } else {
            changeEnabled.disableLoginAccount(currentActorProvider.getCurrentActor(), userId);
        }
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<Void> startLoginAccountPasswordReset(UUID userId) {
        startPasswordReset.startPasswordReset(currentActorProvider.getCurrentActor(), userId);
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).build();
    }
}
