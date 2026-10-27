package com.yarnshipment.discrepancy.application.port.in;

import com.yarnshipment.discrepancy.domain.model.Discrepancy;

/** Uyuşmazlık detayını getirir. */
public interface GetDiscrepancyUseCase {

    Discrepancy getDiscrepancy(GetDiscrepancyQuery query);
}
