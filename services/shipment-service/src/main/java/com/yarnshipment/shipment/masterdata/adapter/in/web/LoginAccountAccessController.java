package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.api.LoginAccountAccessApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.ChangeLoginAccountEnabledRequest;
import com.yarnshipment.shipment.adapter.in.web.api.model.SetLoginAccountPasswordRequest;
import com.yarnshipment.shipment.masterdata.application.port.in.account.DisableLoginAccountUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.account.EnableLoginAccountUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.account.SetLoginAccountPasswordUseCase;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * Giriş hesabının erişimi: pasife alma, yeniden açma ve yöneticinin yeni geçici şifre belirlemesi; yalnızca
 * USER_MANAGE yetkisi olan yönetici. Kişi bir sonraki girişte şifresini kendisi değiştirir. Method security proxy'si
 * gerektirdiği için final değildir (SEC-02).
 */
@RestController
public class LoginAccountAccessController implements LoginAccountAccessApi {

    private final EnableLoginAccountUseCase enableAccount;
    private final DisableLoginAccountUseCase disableAccount;
    private final SetLoginAccountPasswordUseCase setPassword;
    private final CurrentActorProvider currentActorProvider;

    public LoginAccountAccessController(
            EnableLoginAccountUseCase enableAccount,
            DisableLoginAccountUseCase disableAccount,
            SetLoginAccountPasswordUseCase setPassword,
            CurrentActorProvider currentActorProvider) {
        this.enableAccount = enableAccount;
        this.disableAccount = disableAccount;
        this.setPassword = setPassword;
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
    public ResponseEntity<Void> setLoginAccountPassword(UUID userId, SetLoginAccountPasswordRequest request) {
        setPassword.setLoginAccountPassword(currentActorProvider.getCurrentActor(), userId, request.getPassword());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
