package com.yarnshipment.discrepancy.application.port.out;

import com.yarnshipment.discrepancy.domain.rule.SettingVersion;
import com.yarnshipment.discrepancy.domain.rule.TimeLimit;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Durumda kalma süresi sınırı; tolerans kuralı gibi geçerlilik aralığıyla saklanır. */
public interface TimeLimitRulePort {

    Optional<TimeLimit> findTimeLimit(Instant at);

    /** Sınırın sürümleri, en yenisi önce. */
    List<SettingVersion<TimeLimit>> findTimeLimitHistory(int limit);

    /** Geçerli sınırı verilen anda kapatır ve yenisini o andan geçerli yazar. */
    void replaceTimeLimit(TimeLimit limit, UUID actorId, Instant at);
}
