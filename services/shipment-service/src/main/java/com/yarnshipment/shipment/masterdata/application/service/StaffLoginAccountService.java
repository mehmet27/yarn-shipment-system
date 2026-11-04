package com.yarnshipment.shipment.masterdata.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.application.port.in.ChangeLoginAccountEnabledUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.ChangeLoginAccountRoleUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.CreateStaffLoginAccountUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.IssueActivationCodeUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.StaffAccountRequest;
import com.yarnshipment.shipment.masterdata.application.port.out.AccountCredentialPort;
import com.yarnshipment.shipment.masterdata.application.port.out.LoginAccountTargetPort;
import com.yarnshipment.shipment.masterdata.application.port.out.StaffAccountPort;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import com.yarnshipment.shipment.masterdata.domain.model.PhoneNumber;
import com.yarnshipment.shipment.masterdata.domain.model.StaffAccountSpec;
import com.yarnshipment.shipment.masterdata.domain.model.StaffRole;
import java.util.HashSet;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Fabrika içi hesap açar, rolünü değiştirir, pasife alır ve yeniden açar. Şifre üretilmez; kişiye şifre belirleme kodu
 * SMS'le gider. Kişi kendi rolünü değiştiremez ve kendi hesabını kapatamaz (kilitlenmeyi önler). Yerel veritabanına
 * yazılmadığı için transaction açılmaz.
 */
@Service
public final class StaffLoginAccountService
        implements CreateStaffLoginAccountUseCase, ChangeLoginAccountRoleUseCase, ChangeLoginAccountEnabledUseCase {

    private final LoginAccountTargetPort targets;
    private final StaffAccountPort accounts;
    private final AccountCredentialPort credentials;
    private final IssueActivationCodeUseCase activation;

    public StaffLoginAccountService(
            LoginAccountTargetPort targets,
            StaffAccountPort accounts,
            AccountCredentialPort credentials,
            IssueActivationCodeUseCase activation) {
        this.targets = targets;
        this.accounts = accounts;
        this.credentials = credentials;
        this.activation = activation;
    }

    @Override
    public void createStaffLoginAccount(Actor actor, StaffAccountRequest request) {
        if (request.siteIds().isEmpty()
                || !targets.findActiveSiteIds(request.siteIds()).containsAll(new HashSet<>(request.siteIds()))) {
            throw new InvalidMasterDataException("siteIds", "empty or unknown or inactive");
        }
        PhoneNumber phone = PhoneNumber.parse(request.phone());
        UUID userId = accounts.createStaff(new StaffAccountSpec(
                request.username(), request.email(), phone, request.fullName(), request.role(), request.siteIds()));
        activation.issueActivationCode(userId, request.username(), phone);
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
