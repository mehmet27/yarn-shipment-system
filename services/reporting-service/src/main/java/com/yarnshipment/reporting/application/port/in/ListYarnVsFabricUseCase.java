package com.yarnshipment.reporting.application.port.in;

import com.yarnshipment.reporting.domain.model.YarnVsFabricRow;
import java.util.List;

/** Giden iplik ↔ gelen kumaş, fasoncu bazında. */
public interface ListYarnVsFabricUseCase {

    List<YarnVsFabricRow> listYarnVsFabric(ReportQuery query);
}
