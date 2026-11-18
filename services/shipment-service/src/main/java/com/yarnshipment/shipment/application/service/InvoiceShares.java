package com.yarnshipment.shipment.application.service;

import com.yarnshipment.shipment.application.port.in.fire.FireSettlementDetails;
import com.yarnshipment.shipment.application.port.in.fire.MarkInvoicedCommand;
import com.yarnshipment.shipment.application.port.out.ledger.FireInvoicePort.InvoiceRecord;
import com.yarnshipment.shipment.domain.balance.BalanceMovement.InvoiceShare;
import com.yarnshipment.shipment.domain.fabric.YarnDistribution;
import com.yarnshipment.shipment.domain.fabric.YarnShare;
import com.yarnshipment.shipment.domain.fire.FireCalculation;
import com.yarnshipment.shipment.domain.fire.InvalidFireSettlementActionException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Faturanın iplik paylarını belirler: satır varsa o satırlar, yoksa kalan kilo iplik oranıyla paylaştırılır. */
final class InvoiceShares {

    private InvoiceShares() {}

    /** Fatura kaydı: iplik payları ve fasoncu bakiyesine yazılacak bağlam. */
    static InvoiceRecord record(MarkInvoicedCommand command, FireSettlementDetails details, Instant at) {
        FireCalculation calculation = details.calculation();
        UUID shipmentId = command.shipmentId();
        if (!calculation.hasFaultyKg()) {
            throw new InvalidFireSettlementActionException(shipmentId, "NOTHING_TO_INVOICE");
        }
        List<YarnShare> yarns = command.lines().isEmpty()
                ? YarnDistribution.proportional(calculation.remainingKg(), details.yarns())
                : YarnDistribution.explicit(command.lines(), details.yarns(), shipmentId);
        return new InvoiceRecord(
                shipmentId,
                details.settlement().subcontractorId(),
                command.invoiceNo().strip(),
                command.invoicedOn(),
                command.note(),
                yarns.stream()
                        .map(share -> new InvoiceShare(share.yarnType(), share.netKg()))
                        .toList(),
                command.actor().userId(),
                at);
    }

    static BigDecimal totalKg(InvoiceRecord record) {
        return record.lines().stream().map(InvoiceShare::netKg).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
