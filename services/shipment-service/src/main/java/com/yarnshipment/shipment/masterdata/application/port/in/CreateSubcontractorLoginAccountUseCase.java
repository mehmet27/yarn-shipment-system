package com.yarnshipment.shipment.masterdata.application.port.in;

import com.yarnshipment.common.security.Actor;
import java.util.UUID;

/** Etkin fasoncu için Keycloak'ta giriş hesabı açar. */
public interface CreateSubcontractorLoginAccountUseCase {

    void createSubcontractorLoginAccount(Actor actor, UUID subcontractorId, LoginAccountRequest request);
}
