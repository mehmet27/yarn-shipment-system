package com.yarnshipment.reporting.application.port.in;

import com.yarnshipment.reporting.domain.model.PartyStats;
import java.util.List;

/** Şoför bazında göstergeleri listeler. */
public interface ListDriverReportUseCase {

    List<PartyStats> listDriverReport(ReportQuery query);
}
