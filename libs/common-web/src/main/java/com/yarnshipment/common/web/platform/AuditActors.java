package com.yarnshipment.common.web.platform;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * {@code platform.audit_local} trigger'ının okuduğu aktörü yürüyen transaction'a yazar (PERS-23). Ayar transaction
 * yereldir ({@code set_config(..., true)}): commit ya da rollback ile silinir, havuzdaki bağlantıya sızmaz.
 */
public final class AuditActors {

    private static final String SET_ACTOR = "SELECT set_config('app.actor_id', ?, true)";

    private AuditActors() {}

    /**
     * Bu transaction'da yazılacak satırların denetim kaydına işlenecek kullanıcıyı ayarlar.
     *
     * @param jdbc yazma işleminin kullandığı bağlantı
     * @param actorId işlemi yapan kullanıcının kimliği
     */
    public static void actAs(JdbcClient jdbc, UUID actorId) {
        jdbc.sql(SET_ACTOR).param(actorId.toString()).query().singleValue();
    }
}
