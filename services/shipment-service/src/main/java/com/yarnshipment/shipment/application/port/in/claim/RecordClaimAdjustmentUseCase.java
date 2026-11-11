package com.yarnshipment.shipment.application.port.in.claim;

/** Haklı kalite itirazının kilosunu fasoncunun iplik bakiyesinden düşer (KR-15). */
public interface RecordClaimAdjustmentUseCase {

    /** Aynı uyuşmazlık için tekrar çağrılırsa yeni hareket yazmaz. */
    void recordClaimAdjustment(RecordClaimAdjustmentCommand command);
}
