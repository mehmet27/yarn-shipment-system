package com.yarnshipment.discrepancy.application.port.in.claim;

import com.yarnshipment.common.security.Actor;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Kontrol sonucunu girme komutu.
 *
 * @param actor sonucu giren (kalite ya da admin)
 * @param claimId itiraz
 * @param inspectionId kontrol
 * @param summary sonucun özeti
 * @param evidenceId sonuç belgesi ({@code LAB_RESULT}); lab için zorunlu
 */
public record RecordInspectionResultCommand(
        Actor actor,
        UUID claimId,
        UUID inspectionId,
        String summary,
        @Nullable UUID evidenceId) {}
