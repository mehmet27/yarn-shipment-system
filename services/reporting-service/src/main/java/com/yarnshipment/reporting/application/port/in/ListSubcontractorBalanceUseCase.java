package com.yarnshipment.reporting.application.port.in;

import com.yarnshipment.reporting.domain.model.SubcontractorBalance;
import java.util.List;

/** Fasoncu bakiye raporunu listeler; yolda kalan miktarı en yüksek olan önce gelir. */
public interface ListSubcontractorBalanceUseCase {

    List<SubcontractorBalance> listSubcontractorBalance(ReportQuery query);
}
