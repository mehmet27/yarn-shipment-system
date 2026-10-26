package com.yarnshipment.discrepancy.application.port.out;

import com.yarnshipment.discrepancy.domain.model.ToleranceBands;
import java.time.Instant;
import java.util.Optional;

/** Geçerli tolerans kuralı. Faz 1'de ürün bazlı kural yoktur, genel kural okunur. */
public interface ToleranceRulePort {

    Optional<ToleranceBands> findGeneralRule(Instant at);
}
