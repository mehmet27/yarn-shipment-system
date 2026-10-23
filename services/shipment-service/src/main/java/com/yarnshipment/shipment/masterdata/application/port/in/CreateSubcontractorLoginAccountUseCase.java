package com.yarnshipment.shipment.masterdata.application.port.in;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccount;
import java.util.UUID;

/** Etkin fasoncu için Keycloak'ta giriş hesabı açar. */
public interface CreateSubcontractorLoginAccountUseCase {

    LoginAccount createSubcontractorLoginAccount(Actor actor, UUID subcontractorId, LoginAccountRequest request);
}
