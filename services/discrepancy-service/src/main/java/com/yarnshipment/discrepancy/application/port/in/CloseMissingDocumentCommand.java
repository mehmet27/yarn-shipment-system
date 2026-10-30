package com.yarnshipment.discrepancy.application.port.in;

import java.util.Objects;
import java.util.UUID;

/**
 * Sonradan yüklenen imzalı teslim belgesi.
 *
 * @param shipmentId sevkiyat
 * @param evidenceId yüklenen belge; karar notuna yazılır
 */
public record CloseMissingDocumentCommand(UUID shipmentId, UUID evidenceId) {

    public CloseMissingDocumentCommand {
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(evidenceId, "evidenceId");
    }
}
