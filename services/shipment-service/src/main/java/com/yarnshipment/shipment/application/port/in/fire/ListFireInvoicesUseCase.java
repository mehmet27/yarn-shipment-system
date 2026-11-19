package com.yarnshipment.shipment.application.port.in.fire;

/** Faturası girilmiş fire dosyası faturalarını sayfalı okur (BALANCE_VIEW). */
public interface ListFireInvoicesUseCase {

    FireInvoicePage listFireInvoices(ListFireInvoicesQuery query);
}
