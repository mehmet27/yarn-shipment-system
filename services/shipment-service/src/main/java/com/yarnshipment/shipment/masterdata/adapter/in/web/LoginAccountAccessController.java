package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.api.LoginAccountAccessApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.ChangeLoginAccountEnabledRequest;
import com.yarnshipment.shipment.masterdata.application.port.in.DisableLoginAccountUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.EnableLoginAccountUseCase;
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

    private final EnableLoginAccountUseCase enableAccount;
    private final DisableLoginAccountUseCase disableAccount;
    private final StartPasswordResetUseCase startPasswordReset;
    private final CurrentActorProvider currentActorProvider;

    public LoginAccountAccessController(
            EnableLoginAccountUseCase enableAccount,
            DisableLoginAccountUseCase disableAccount,
            StartPasswordResetUseCase startPasswordReset,
            CurrentActorProvider currentActorProvider) {
        this.enableAccount = enableAccount;
        this.disableAccount = disableAccount;
        this.startPasswordReset = startPasswordReset;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<Void> changeLoginAccountEnabled(UUID userId, ChangeLoginAccountEnabledRequest request) {
        if (request.getEnabled()) {
            enableAccount.enableLoginAccount(currentActorProvider.getCurrentActor(), userId);
        } else {
            disableAccount.disableLoginAccount(currentActorProvider.getCurrentActor(), userId);
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
