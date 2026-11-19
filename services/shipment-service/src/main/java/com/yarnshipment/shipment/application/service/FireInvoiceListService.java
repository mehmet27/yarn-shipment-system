package com.yarnshipment.shipment.application.service;

import com.yarnshipment.shipment.application.port.in.fire.FireInvoicePage;
import com.yarnshipment.shipment.application.port.in.fire.ListFireInvoicesQuery;
import com.yarnshipment.shipment.application.port.in.fire.ListFireInvoicesUseCase;
import com.yarnshipment.shipment.application.port.out.ledger.FireInvoiceListPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Faturalananlar listesi: tarih ve fasoncu süzgeciyle girilmiş faturalar. */
@Service
public class FireInvoiceListService implements ListFireInvoicesUseCase {

    private final FireInvoiceListPort invoices;

    public FireInvoiceListService(FireInvoiceListPort invoices) {
        this.invoices = invoices;
    }

    @Override
    @Transactional(readOnly = true)
    public FireInvoicePage listFireInvoices(ListFireInvoicesQuery query) {
        return new FireInvoicePage(
                invoices.findInvoices(query),
                query.page(),
                query.size(),
                invoices.countInvoices(query),
                invoices.sumInvoiceKg(query));
    }
}
