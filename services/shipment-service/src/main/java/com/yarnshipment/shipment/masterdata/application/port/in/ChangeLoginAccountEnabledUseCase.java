package com.yarnshipment.shipment.masterdata.application.port.in;

import com.yarnshipment.common.security.Actor;
import java.util.UUID;

/** Giriş hesabını pasife alır ya da yeniden açar. */
public interface ChangeLoginAccountEnabledUseCase {

    /** Hesabı yeniden girişe açar. */
    void enableLoginAccount(Actor actor, UUID userId);

    /** Hesabı kapatır; kişi kendi hesabını kapatamaz ({@code InvalidMasterDataException}, alan {@code userId}). */
    void disableLoginAccount(Actor actor, UUID userId);
}
