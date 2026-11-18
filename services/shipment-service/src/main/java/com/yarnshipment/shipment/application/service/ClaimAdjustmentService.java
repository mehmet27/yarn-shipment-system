package com.yarnshipment.shipment.application.service;

import com.yarnshipment.shipment.application.port.in.claim.RecordClaimAdjustmentCommand;
import com.yarnshipment.shipment.application.port.in.claim.RecordClaimAdjustmentUseCase;
import com.yarnshipment.shipment.application.port.out.ledger.BalanceQueryPort;
import com.yarnshipment.shipment.application.port.out.ledger.ClaimLedgerPort;
import com.yarnshipment.shipment.application.port.out.ledger.ClaimLedgerPort.InvoiceableClaim;
import com.yarnshipment.shipment.application.port.out.ledger.FireSettlementPort;
import com.yarnshipment.shipment.domain.balance.BalanceMovement;
import com.yarnshipment.shipment.domain.balance.BalanceMovement.ClaimContext;
import com.yarnshipment.shipment.domain.balance.BalanceMovement.ClaimShare;
import com.yarnshipment.shipment.domain.balance.Kilograms;
import com.yarnshipment.shipment.domain.fabric.YarnDistribution;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Haklı kalite itirazının kilosunu işler (KR-15, analiz 5.12). Fire dosyası açıksa kilo bakiyeden hemen düşülmez:
 * fire dosyasında faturalanacak kiloya eklenir ve müdür fatura kesince fasoncunun iplik bakiyesinden düşer. Dosya
 * yoksa ya da kapanmışsa (fatura bekleyemez) kilo, sevkiyatın gönderilen iplik cinslerine gönderim oranıyla dağıtılıp
 * bakiyeden hemen düşülür. Fire hesabı itiraz kilosunu her iki durumda da fire tabanından düşer. Aynı uyuşmazlığın
 * olayı tekrar gelirse kayıt değişmez.
 */
@Service
public class ClaimAdjustmentService implements RecordClaimAdjustmentUseCase {

    private final ClaimLedgerPort ledger;
    private final BalanceQueryPort balances;
    private final FireSettlementPort settlements;

    public ClaimAdjustmentService(ClaimLedgerPort ledger, BalanceQueryPort balances, FireSettlementPort settlements) {
        this.ledger = ledger;
        this.balances = balances;
        this.settlements = settlements;
    }

    @Override
    @Transactional
    public void recordClaimAdjustment(RecordClaimAdjustmentCommand command) {
        UUID shipmentId = command.shipmentId();
        UUID subcontractorId = ledger.findSubcontractor(shipmentId).orElse(null);
        Map<String, BigDecimal> dispatched = balances.findDispatchedByYarn(shipmentId);
        if (subcontractorId == null || dispatched.isEmpty()) {
            // Bakiyesi olmayan sevkiyatın (fasoncusuz ya da hiç tamamlanmamış) düşülecek iplik bakiyesi yoktur.
            return;
        }
        BigDecimal claimedKg = Kilograms.round(command.claimedKg());
        if (isInvoiceable(shipmentId)) {
            ledger.recordInvoiceableClaim(new InvoiceableClaim(
                    command.discrepancyId(), shipmentId, claimedKg, command.decidedAt(), command.decidedBy()));
        } else {
            ClaimContext context = new ClaimContext(
                    command.discrepancyId(), subcontractorId, shipmentId, command.decidedAt(), command.decidedBy());
            YarnDistribution.proportional(claimedKg, dispatched).stream()
                    .map(share -> new ClaimShare(share.yarnType(), share.netKg()))
                    .map(share -> BalanceMovement.claimAdjustment(share, context))
                    .forEach(ledger::append);
        }
    }

    /** Açık fire dosyası itiraz kilosunu faturalanacak kilo olarak taşıyabilir; kapanmış dosya taşıyamaz. */
    private boolean isInvoiceable(UUID shipmentId) {
        return settlements
                .find(shipmentId)
                .filter(file -> !file.status().isFinal())
                .isPresent();
    }
}
