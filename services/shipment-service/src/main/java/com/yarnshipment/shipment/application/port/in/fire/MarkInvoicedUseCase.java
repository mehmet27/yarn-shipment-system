package com.yarnshipment.shipment.application.port.in.fire;

/** Hatalı kg'si olan fire dosyasını "fatura edildi" işaretler (INVOICE_MARK); geri alınamaz. */
public interface MarkInvoicedUseCase {

    FireSettlementDetails markInvoiced(MarkInvoicedCommand command);
}
