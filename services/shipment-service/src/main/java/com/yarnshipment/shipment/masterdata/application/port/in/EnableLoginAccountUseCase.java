package com.yarnshipment.shipment.masterdata.application.port.in;

import com.yarnshipment.common.security.Actor;
import java.util.UUID;

/** Pasife alınmış giriş hesabını yeniden girişe açar. */
public interface EnableLoginAccountUseCase {

    void enableLoginAccount(Actor actor, UUID userId);
}
