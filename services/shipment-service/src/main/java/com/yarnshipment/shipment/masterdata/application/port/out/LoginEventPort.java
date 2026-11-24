package com.yarnshipment.shipment.masterdata.application.port.out;

import com.yarnshipment.shipment.masterdata.domain.model.AuthUser;
import java.time.Instant;

/** Giriş olaylarını denetim kaydı için yayınlar: giren kişi, yanlış şifre denenen hesap, kilitlenen hesap. */
public interface LoginEventPort {

    void publishSucceeded(AuthUser user, Instant now);

    void publishFailed(AuthUser user, Instant now);

    void publishLocked(AuthUser user, Instant lockedUntil, Instant now);
}
