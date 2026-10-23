package com.yarnshipment.shipment.masterdata.application.port.in;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccount;
import java.util.UUID;

/** Etkin şoför için Keycloak'ta giriş hesabı açar. */
public interface CreateDriverLoginAccountUseCase {

    LoginAccount createDriverLoginAccount(Actor actor, UUID driverId, LoginAccountRequest request);
}
