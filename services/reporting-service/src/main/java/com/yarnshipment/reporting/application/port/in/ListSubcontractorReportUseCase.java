package com.yarnshipment.reporting.application.port.in;

import com.yarnshipment.reporting.domain.model.PartyStats;
import java.util.List;

/** Fasoncu bazında göstergeleri listeler. */
public interface ListSubcontractorReportUseCase {

    List<PartyStats> listSubcontractorReport(ReportQuery query);
}
