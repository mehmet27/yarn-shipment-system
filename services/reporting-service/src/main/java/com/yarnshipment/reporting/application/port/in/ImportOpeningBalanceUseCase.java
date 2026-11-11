package com.yarnshipment.reporting.application.port.in;

import com.yarnshipment.reporting.domain.model.OpeningBalanceRow;
import com.yarnshipment.reporting.domain.model.OpeningImportResult;
import java.time.LocalDate;
import java.util.List;

/** Açılış bakiyesini OPENING hareketi olarak yazar; (fasoncu, iplik) için açılış varsa satırı atlar. */
public interface ImportOpeningBalanceUseCase {

    OpeningImportResult importOpening(LocalDate asOf, List<OpeningBalanceRow> rows);
}
