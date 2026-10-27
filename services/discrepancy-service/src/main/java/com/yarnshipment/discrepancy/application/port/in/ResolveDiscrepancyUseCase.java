package com.yarnshipment.discrepancy.application.port.in;

import com.yarnshipment.discrepancy.domain.model.Discrepancy;

/** Uyuşmazlığı karara bağlar; sevkiyatın açık uyuşmazlığı kalmadıysa olay bunu bildirir. */
public interface ResolveDiscrepancyUseCase {

    Discrepancy resolveDiscrepancy(ResolveDiscrepancyCommand command);
}
