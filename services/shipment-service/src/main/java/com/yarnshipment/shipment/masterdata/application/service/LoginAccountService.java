package com.yarnshipment.shipment.masterdata.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.application.port.in.CreateDriverLoginAccountUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.CreateSubcontractorLoginAccountUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.LoginAccountRequest;
import com.yarnshipment.shipment.masterdata.application.port.out.LoginAccountPort;
import com.yarnshipment.shipment.masterdata.application.port.out.LoginAccountTargetPort;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccount;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSpec;
import com.yarnshipment.shipment.masterdata.domain.model.LoginRole;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataNotFoundException;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Şoför ve fasoncu için giriş hesabı açar. Kayıt ve lokasyonlar etkin olmalıdır; hesap Keycloak'ta tek çağrı dizisiyle
 * açılır ve yarım kalırsa geri alınır. Yerel veritabanına yazılmadığı için transaction açılmaz.
 */
@Service
public final class LoginAccountService
        implements CreateDriverLoginAccountUseCase, CreateSubcontractorLoginAccountUseCase {

    private final LoginAccountTargetPort targets;
    private final LoginAccountPort accounts;

    public LoginAccountService(LoginAccountTargetPort targets, LoginAccountPort accounts) {
        this.targets = targets;
        this.accounts = accounts;
    }

    @Override
    public LoginAccount createDriverLoginAccount(Actor actor, UUID driverId, LoginAccountRequest request) {
        String name = targets.findActiveDriverName(driverId)
                .orElseThrow(() -> new MasterDataNotFoundException("Driver", driverId));
        return open(LoginRole.DRIVER, driverId, name, request);
    }

    @Override
    public LoginAccount createSubcontractorLoginAccount(
            Actor actor, UUID subcontractorId, LoginAccountRequest request) {
        Optional<String> title = targets.findActiveSubcontractorTitle(subcontractorId);
        return open(
                LoginRole.SUBCONTRACTOR,
                subcontractorId,
                title.orElseThrow(() -> new MasterDataNotFoundException("Subcontractor", subcontractorId)),
                request);
    }

    private LoginAccount open(LoginRole role, UUID ownerId, String displayName, LoginAccountRequest request) {
        if (request.siteIds().isEmpty()
                || !targets.findActiveSiteIds(request.siteIds()).containsAll(new HashSet<>(request.siteIds()))) {
            throw new InvalidMasterDataException("siteIds", "empty or unknown or inactive");
        }
        String password = TemporaryPasswords.generate();
        accounts.create(new LoginAccountSpec(
                request.username(), request.email(), displayName, role, ownerId, request.siteIds(), password));
        return new LoginAccount(request.username(), password);
    }
}
