package com.yarnshipment.discrepancy.application.port.in;

import com.yarnshipment.discrepancy.domain.model.Discrepancy;

/** Uyuşmazlığı bir sorumluya atar ve incelemeye alır. */
public interface AssignDiscrepancyUseCase {

    Discrepancy assignDiscrepancy(AssignDiscrepancyCommand command);
}
