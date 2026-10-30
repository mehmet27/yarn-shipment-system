package com.yarnshipment.discrepancy.application.port.in.claim;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.discrepancy.domain.claim.InspectionKind;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * İtiraz için numune ya da lab kontrolü isteme komutu.
 *
 * @param actor isteyen (kalite ya da admin)
 * @param claimId itiraz
 * @param kind numune ya da lab
 * @param note isteğin notu
 */
public record RequestInspectionCommand(
        Actor actor,
        UUID claimId,
        InspectionKind kind,
        @Nullable String note) {}
