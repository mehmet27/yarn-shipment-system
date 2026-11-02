package com.yarnshipment.evidence.application.port.out;

import com.yarnshipment.evidence.domain.model.EvidenceFile;
import java.time.Instant;
import java.util.UUID;

/** Kanıtın geçersiz kılınması. */
public interface EvidenceInvalidationPort {

    /**
     * Geçersiz kılınmış kaydı ve {@code EvidenceInvalidated} olayını aynı transaction'da yazar. Kayıt zaten geçersizse
     * {@code EvidenceAlreadyInvalidatedException} fırlatır.
     */
    void saveInvalidation(EvidenceFile evidence, UUID invalidatedBy, Instant at);
}
