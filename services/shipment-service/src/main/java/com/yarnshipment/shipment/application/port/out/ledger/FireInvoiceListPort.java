package com.yarnshipment.shipment.application.port.out.ledger;

import com.yarnshipment.shipment.application.port.in.fire.FireInvoicePage;
import com.yarnshipment.shipment.application.port.in.fire.ListFireInvoicesQuery;
import java.math.BigDecimal;
import java.util.List;

/** Girilmiş fire dosyası faturalarını tüm sevkiyatlar boyunca listeler; yeniden eskiye. */
public interface FireInvoiceListPort {

    List<FireInvoicePage.Row> findInvoices(ListFireInvoicesQuery query);

    long countInvoices(ListFireInvoicesQuery query);

    /** Süzgece uyan tüm faturaların toplam kilosu. */
    BigDecimal sumInvoiceKg(ListFireInvoicesQuery query);
}
