package com.yarnshipment.reporting.application.port.in;

import com.yarnshipment.reporting.domain.model.BalanceMatrixRow;
import java.util.List;

/** Bakiye matrisi. */
public interface ListBalanceMatrixUseCase {

    List<BalanceMatrixRow> listBalanceMatrix(ReportQuery query);
}
