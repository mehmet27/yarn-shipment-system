package com.yarnshipment.shipment.masterdata.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.application.port.in.ChangeLoginAccountRoleUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.CreateStaffLoginAccountUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.StaffAccountRequest;
import com.yarnshipment.shipment.masterdata.application.port.out.LoginAccountTargetPort;
import com.yarnshipment.shipment.masterdata.application.port.out.StaffAccountPort;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import com.yarnshipment.shipment.masterdata.domain.model.StaffAccountSpec;
import com.yarnshipment.shipment.masterdata.domain.model.StaffRole;
import java.util.HashSet;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Fabrika içi hesap açar ve rolünü değiştirir. Şifre üretilmez; kişiye şifre belirleme e-postası gider. Kişi kendi
 * rolünü değiştiremez (yetkisini düşürüp kilitlenmeyi önler). Yerel veritabanına yazılmadığı için transaction açılmaz.
 */
@Service
public final class StaffLoginAccountService implements CreateStaffLoginAccountUseCase, ChangeLoginAccountRoleUseCase {

    private final LoginAccountTargetPort targets;
    private final StaffAccountPort accounts;

    public StaffLoginAccountService(LoginAccountTargetPort targets, StaffAccountPort accounts) {
        this.targets = targets;
        this.accounts = accounts;
    }

    @Override
    public void createStaffLoginAccount(Actor actor, StaffAccountRequest request) {
        if (request.siteIds().isEmpty()
                || !targets.findActiveSiteIds(request.siteIds()).containsAll(new HashSet<>(request.siteIds()))) {
            throw new InvalidMasterDataException("siteIds", "empty or unknown or inactive");
        }
        accounts.createStaff(new StaffAccountSpec(
                request.username(), request.email(), request.fullName(), request.role(), request.siteIds()));
    }

    @Override
    public void changeLoginAccountRole(Actor actor, UUID userId, StaffRole role) {
        if (actor.userId().equals(userId)) {
            throw new InvalidMasterDataException("userId", "own account");
        }
        accounts.changeStaffRole(userId, role);
    }
}
