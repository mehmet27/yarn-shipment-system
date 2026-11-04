package com.yarnshipment.shipment.masterdata.application.port.in;

import com.yarnshipment.common.security.Actor;
import java.util.UUID;

/** Giriş hesabını pasife alır ya da yeniden açar. */
public interface ChangeLoginAccountEnabledUseCase {

    /** Kişi kendi hesabını kapatamaz ({@code InvalidMasterDataException}, alan {@code userId}). */
    void changeLoginAccountEnabled(Actor actor, UUID userId, boolean enabled);
}
