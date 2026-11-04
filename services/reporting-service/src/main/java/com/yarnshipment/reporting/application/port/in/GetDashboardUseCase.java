package com.yarnshipment.reporting.application.port.in;

import com.yarnshipment.reporting.domain.model.Dashboard;

/** Panel ana sayfa özetini verir. */
public interface GetDashboardUseCase {

    Dashboard getDashboard(ReportQuery query);
}
