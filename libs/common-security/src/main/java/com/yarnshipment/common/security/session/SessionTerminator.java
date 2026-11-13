package com.yarnshipment.common.security.session;

import java.util.UUID;

/** Kullanıcının açık oturumlarını sonlandırır (rol değişti, hesap kapandı, şifre sıfırlandı). */
public interface SessionTerminator {

    /** Kullanıcının tüm oturumları bir sonraki istekte geçersiz olur. */
    void terminateAll(UUID userId);

    /** Kullanıcının, verilen oturum dışındaki oturumlarını sonlandırır. */
    void terminateOthers(UUID userId, String keepSessionId);
}
