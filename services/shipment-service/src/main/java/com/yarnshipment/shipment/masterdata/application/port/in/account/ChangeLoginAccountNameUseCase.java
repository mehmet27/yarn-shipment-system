package com.yarnshipment.shipment.masterdata.application.port.in.account;

import com.yarnshipment.common.security.Actor;
import java.util.UUID;

/** Fabrika içi hesabın ad soyadını değiştirir (yalnızca yönetici); kişi kendi adını da değiştirebilir. */
public interface ChangeLoginAccountNameUseCase {

    /** Kullanıcı yoksa {@code LoginAccountNotFoundException}; ad boşsa {@code InvalidMasterDataException}. */
    void changeLoginAccountName(Actor actor, UUID userId, String fullName);
}
