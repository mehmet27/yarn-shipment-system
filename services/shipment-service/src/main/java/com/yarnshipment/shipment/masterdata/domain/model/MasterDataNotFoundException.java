package com.yarnshipment.shipment.masterdata.domain.model;

import java.io.Serial;
import java.util.UUID;

/** İstenen ana veri kaydı yok (404, {@code MASTER_DATA_NOT_FOUND}). */
public final class MasterDataNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public MasterDataNotFoundException(String kind, UUID id) {
        super("Master data not found kind=" + kind + " id=" + id);
    }
}
