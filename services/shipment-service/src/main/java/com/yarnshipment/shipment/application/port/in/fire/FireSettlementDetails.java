package com.yarnshipment.shipment.application.port.in.fire;

import com.yarnshipment.shipment.application.port.out.ledger.FireInvoicePort.InvoiceEntry;
import com.yarnshipment.shipment.domain.fire.FireCalculation;
import com.yarnshipment.shipment.domain.fire.FireSettlement;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Fire dosyası ve güncel hesabı.
 *
 * @param settlement dosya
 * @param calculation kayıtlardan hesaplanan fire
 * @param yarns sevkiyatta iplik cinsine göre gönderilen kg (fatura satırı seçenekleri)
 * @param invoices şimdiye kadar girilen faturalar
 */
public record FireSettlementDetails(
        FireSettlement settlement,
        FireCalculation calculation,
        Map<String, BigDecimal> yarns,
        List<InvoiceEntry> invoices) {

    /** İplik ve fatura bilgisi olmayan dosya. */
    public FireSettlementDetails(FireSettlement settlement, FireCalculation calculation) {
        this(settlement, calculation, Map.of(), List.of());
    }
}
