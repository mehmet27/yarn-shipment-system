package com.yarnshipment.shipment.masterdata.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.application.port.in.account.ChangeLoginAccountRoleUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.account.CreateStaffLoginAccountUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.account.DisableLoginAccountUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.account.EnableLoginAccountUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.account.SetLoginAccountPasswordUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.account.StaffAccountRequest;
import com.yarnshipment.shipment.masterdata.application.port.out.AccountCredentialPort;
import com.yarnshipment.shipment.masterdata.application.port.out.LoginAccountTargetPort;
import com.yarnshipment.shipment.masterdata.application.port.out.StaffAccountPort;
import com.yarnshipment.shipment.masterdata.application.port.out.StaffWorkAreaPort;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import com.yarnshipment.shipment.masterdata.domain.model.StaffAccountSpec;
import com.yarnshipment.shipment.masterdata.domain.model.StaffRole;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Fabrika içi hesap açar, rolünü değiştirir, pasife alır, yeniden açar ve şifresini belirler. İlk şifreyi yönetici
 * yazar (geçici; kişi ilk girişte değiştirir); SMS ya da e-posta doğrulaması yoktur. Fabrikada tek merkez lokasyon
 * olduğu için yeni hesap etkin tüm lokasyonları görür. Kişi kendi rolünü değiştiremez ve kendi hesabını kapatamaz
 * (kilitlenmeyi önler). Kalite rolünde çalışma alanı (merkez / fasoncu) zorunludur, başka rolde verilemez. Asıl
 * yazım kimlik sağlayıcıda olduğu için transaction açılmaz.
 */
@Service
public final class StaffLoginAccountService
        implements CreateStaffLoginAccountUseCase,
                ChangeLoginAccountRoleUseCase,
                EnableLoginAccountUseCase,
                DisableLoginAccountUseCase,
                SetLoginAccountPasswordUseCase {

    private final LoginAccountTargetPort targets;
    private final StaffAccountPort accounts;
    private final AccountCredentialPort credentials;
    private final StaffWorkAreaPort workAreas;

    public StaffLoginAccountService(
            LoginAccountTargetPort targets,
            StaffAccountPort accounts,
            AccountCredentialPort credentials,
            StaffWorkAreaPort workAreas) {
        this.targets = targets;
        this.accounts = accounts;
        this.credentials = credentials;
        this.workAreas = workAreas;
    }

    @Override
    public void createStaffLoginAccount(Actor actor, StaffAccountRequest request) {
        List<UUID> siteIds = new ArrayList<>(targets.findAllActiveSiteIds());
        if (siteIds.isEmpty()) {
            throw new InvalidMasterDataException("siteIds", "no active site");
        }
        boolean quality = request.role() == StaffRole.QUALITY;
        if (quality != (request.workArea() != null)) {
            throw new InvalidMasterDataException("workArea", quality ? "required for quality" : "only for quality");
        }
        UUID userId = accounts.createStaff(new StaffAccountSpec(
                request.username(), request.fullName(), request.role(), request.password(), siteIds));
        if (request.workArea() != null) {
            workAreas.save(userId, request.workArea());
        }
    }

    @Override
    public void setLoginAccountPassword(Actor actor, UUID userId, String password) {
        credentials.setTemporaryPassword(userId, password);
    }

    @Override
    public void changeLoginAccountRole(Actor actor, UUID userId, StaffRole role) {
        if (actor.userId().equals(userId)) {
            throw new InvalidMasterDataException("userId", "own account");
        }
        accounts.changeStaffRole(userId, role);
    }

    @Override
    public void enableLoginAccount(Actor actor, UUID userId) {
        credentials.enable(userId);
    }

    @Override
    public void disableLoginAccount(Actor actor, UUID userId) {
        if (actor.userId().equals(userId)) {
            throw new InvalidMasterDataException("userId", "own account");
        }
        credentials.disable(userId);
    }
}
