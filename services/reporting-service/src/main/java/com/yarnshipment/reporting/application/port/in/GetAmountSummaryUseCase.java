package com.yarnshipment.reporting.application.port.in;

import com.yarnshipment.reporting.domain.model.AmountSummary;

/** Müdür ve genel müdür için tutar bilgisi. */
public interface GetAmountSummaryUseCase {

    AmountSummary getAmountSummary(ReportQuery query);
}
