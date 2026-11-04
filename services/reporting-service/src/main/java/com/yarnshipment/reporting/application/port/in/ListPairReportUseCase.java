package com.yarnshipment.reporting.application.port.in;

import com.yarnshipment.reporting.domain.model.PairStats;
import java.util.List;

/** Şoför–fasoncu eşleşmesi bazında göstergeleri listeler. */
public interface ListPairReportUseCase {

    List<PairStats> listPairReport(ReportQuery query);
}
