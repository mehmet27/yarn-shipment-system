package com.yarnshipment.shipment.masterdata.application.port.in.account;

import com.yarnshipment.common.security.Actor;
import java.util.UUID;

/** Yönetici bir kullanıcı için şifre sıfırlamayı başlatır; kişiye SMS kodu gider, şifreyi kişi belirler. */
public interface StartPasswordResetUseCase {

    /**
     * Kullanıcı yoksa {@code LoginAccountNotFoundException}; telefonu yoksa {@code InvalidMasterDataException}
     * (alan {@code phone}); SMS gidemezse {@code SmsUnavailableException} fırlatır.
     */
    void startPasswordReset(Actor actor, UUID userId);
}
