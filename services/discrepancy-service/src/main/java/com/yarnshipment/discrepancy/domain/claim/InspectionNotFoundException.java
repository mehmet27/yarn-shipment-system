package com.yarnshipment.discrepancy.domain.claim;

import java.util.UUID;

/** Bu kimlikte, bu itiraza bağlı kontrol yok. */
public final class InspectionNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InspectionNotFoundException(UUID inspectionId) {
        super("Inspection not found id=" + inspectionId);
    }
}
