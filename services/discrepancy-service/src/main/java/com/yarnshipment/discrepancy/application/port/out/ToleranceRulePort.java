package com.yarnshipment.discrepancy.application.port.out;

import com.yarnshipment.discrepancy.domain.model.ToleranceBands;
import com.yarnshipment.discrepancy.domain.rule.SettingVersion;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Geçerli tolerans kuralı. Faz 1'de ürün bazlı kural yoktur, genel kural okunur ve değiştirilir. */
public interface ToleranceRulePort {

    Optional<ToleranceBands> findGeneralRule(Instant at);

    /** Genel kuralın sürümleri, en yenisi önce. */
    List<SettingVersion<ToleranceBands>> findGeneralRuleHistory(int limit);

    /** Geçerli genel kuralı verilen anda kapatır ve yenisini o andan geçerli yazar. */
    void replaceGeneralRule(ToleranceBands bands, UUID actorId, Instant at);
}
