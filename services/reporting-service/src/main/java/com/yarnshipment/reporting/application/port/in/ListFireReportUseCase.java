package com.yarnshipment.reporting.application.port.in;

import com.yarnshipment.reporting.domain.model.FireFile;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Opsiyonel fasoncu süzgeci ile fire dosyaları. */
public interface ListFireReportUseCase {

    List<FireFile> listFire(ReportQuery query, @Nullable UUID subcontractorId);
}
