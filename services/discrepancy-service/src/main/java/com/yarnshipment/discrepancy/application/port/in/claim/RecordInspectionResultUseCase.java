package com.yarnshipment.discrepancy.application.port.in.claim;

import com.yarnshipment.discrepancy.domain.claim.Inspection;

/** Numune ya da lab sonucunu bir kez girer; lab sonucu belgeyle gelir. */
public interface RecordInspectionResultUseCase {

    Inspection recordInspectionResult(RecordInspectionResultCommand command);
}
