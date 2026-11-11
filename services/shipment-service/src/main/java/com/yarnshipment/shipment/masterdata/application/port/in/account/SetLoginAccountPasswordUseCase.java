package com.yarnshipment.shipment.masterdata.application.port.in.account;

import com.yarnshipment.common.security.Actor;
import java.util.UUID;

/** Yönetici bir hesaba yeni geçici şifre belirler; kişi bir sonraki girişte şifresini değiştirir, açık oturumları kapanır. */
public interface SetLoginAccountPasswordUseCase {

    /** Şifre kurala uymuyorsa {@code WeakPasswordException}; kullanıcı yoksa {@code LoginAccountNotFoundException}. */
    void setLoginAccountPassword(Actor actor, UUID userId, String password);
}
