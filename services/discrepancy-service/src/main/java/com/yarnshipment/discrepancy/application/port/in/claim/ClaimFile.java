package com.yarnshipment.discrepancy.application.port.in.claim;

import com.yarnshipment.discrepancy.domain.claim.Inspection;
import com.yarnshipment.discrepancy.domain.claim.QualityClaim;
import com.yarnshipment.discrepancy.domain.model.Discrepancy;
import java.util.List;
import java.util.Objects;

/**
 * Bir itiraz, uyuşmazlığı ve kontrolleri birlikte.
 *
 * @param claim itiraz
 * @param discrepancy itirazın uyuşmazlığı (durum ve karar)
 * @param inspections istenen numune ve lab kontrolleri
 */
public record ClaimFile(QualityClaim claim, Discrepancy discrepancy, List<Inspection> inspections) {

    public ClaimFile {
        Objects.requireNonNull(claim, "claim");
        Objects.requireNonNull(discrepancy, "discrepancy");
        inspections = List.copyOf(inspections);
    }
}
