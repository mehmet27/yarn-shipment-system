package com.yarnshipment.reporting.application.port.in;

import com.yarnshipment.reporting.domain.model.BalanceMatrixRow;
import com.yarnshipment.reporting.domain.model.FireFile;
import com.yarnshipment.reporting.domain.model.YarnVsFabricRow;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Bakiye matrisi ve fire raporları (analiz 11). */
public interface BalanceFireReportUseCase {

    List<BalanceMatrixRow> listBalanceMatrix(ReportQuery query);

    /** Opsiyonel fasoncu süzgeci ile fire dosyaları. */
    List<FireFile> listFire(ReportQuery query, @Nullable UUID subcontractorId);

    List<YarnVsFabricRow> listYarnVsFabric(ReportQuery query);

    /** Eksik kilosu sıfırdan büyük ve durumu TO_INVOICE olan dosyalar. */
    List<FireFile> listToInvoice(ReportQuery query);
}
