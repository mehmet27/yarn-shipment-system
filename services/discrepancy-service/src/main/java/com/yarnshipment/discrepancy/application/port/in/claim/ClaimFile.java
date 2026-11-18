package com.yarnshipment.discrepancy.application.port.in.claim;

import com.yarnshipment.discrepancy.domain.claim.QualityClaim;
import com.yarnshipment.discrepancy.domain.model.Discrepancy;
import java.util.Objects;

/**
 * Bir itiraz, uyuşmazlığı ve kontrolleri birlikte.
 *
 * @param claim itiraz
 * @param discrepancy itirazın uyuşmazlığı (durum ve karar)
 */
public record ClaimFile(QualityClaim claim, Discrepancy discrepancy) {

    public ClaimFile {
        Objects.requireNonNull(claim, "claim");
        Objects.requireNonNull(discrepancy, "discrepancy");
    }
}
