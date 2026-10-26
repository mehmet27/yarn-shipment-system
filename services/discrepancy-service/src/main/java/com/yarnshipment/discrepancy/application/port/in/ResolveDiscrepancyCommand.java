package com.yarnshipment.discrepancy.application.port.in;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.discrepancy.domain.model.Resolution;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Uyuşmazlığı karara bağlama komutu.
 *
 * @param actor karar veren
 * @param discrepancyId uyuşmazlık
 * @param resolution karar kodu
 * @param note gerekçe
 * @param liableParty sorumlu taraf; yalnızca sorumluluk atanan kararda
 */
public record ResolveDiscrepancyCommand(
        Actor actor,
        UUID discrepancyId,
        Resolution resolution,
        String note,
        @Nullable String liableParty) {}
