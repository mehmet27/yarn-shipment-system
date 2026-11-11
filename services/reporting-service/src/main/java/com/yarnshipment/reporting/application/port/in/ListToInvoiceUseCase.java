package com.yarnshipment.reporting.application.port.in;

import com.yarnshipment.reporting.domain.model.FireFile;
import java.util.List;

/** Eksik kilosu sıfırdan büyük ve durumu TO_INVOICE olan dosyalar. */
public interface ListToInvoiceUseCase {

    List<FireFile> listToInvoice(ReportQuery query);
}
