package com.yarnshipment.shipment.masterdata.application.port.in.account;

import com.yarnshipment.common.security.Actor;
import java.util.UUID;

/** Etkin şoför için Keycloak'ta giriş hesabı açar. */
public interface CreateDriverLoginAccountUseCase {

    void createDriverLoginAccount(Actor actor, UUID driverId, LoginAccountRequest request);
}
