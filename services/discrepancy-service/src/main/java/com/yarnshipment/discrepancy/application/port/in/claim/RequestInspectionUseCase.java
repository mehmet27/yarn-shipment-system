package com.yarnshipment.discrepancy.application.port.in.claim;

import com.yarnshipment.discrepancy.domain.claim.Inspection;

/** Karara bağlanmamış itiraz için numune ya da lab kontrolü ister. */
public interface RequestInspectionUseCase {

    Inspection requestInspection(RequestInspectionCommand command);
}
