package com.yarnshipment.shipment.masterdata.application.port.in;

import com.yarnshipment.common.security.Actor;
import java.util.UUID;

/** Giriş hesabını pasife alır; açık oturumlar kapanır. */
public interface DisableLoginAccountUseCase {

    /** Kişi kendi hesabını kapatamaz ({@code InvalidMasterDataException}, alan {@code userId}). */
    void disableLoginAccount(Actor actor, UUID userId);
}
