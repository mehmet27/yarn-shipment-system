package com.yarnshipment.shipment.application.service;

import com.yarnshipment.shipment.application.port.in.claim.RecordClaimAdjustmentCommand;
import com.yarnshipment.shipment.application.port.in.claim.RecordClaimAdjustmentUseCase;
import com.yarnshipment.shipment.application.port.out.ledger.BalanceQueryPort;
import com.yarnshipment.shipment.application.port.out.ledger.ClaimLedgerPort;
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
 * Haklı kalite itirazının kilosunu fasoncunun iplik bakiyesinden düşer (KR-15, analiz 5.12). Kilo, sevkiyatın
 * gönderilen iplik cinslerine gönderim oranıyla dağıtılır; fire hesabı bu hareketleri fire tabanından düşer. Aynı
 * uyuşmazlığın olayı tekrar gelirse defter değişmez.
 */
@Service
public class ClaimAdjustmentService implements RecordClaimAdjustmentUseCase {

    private final ClaimLedgerPort ledger;
    private final BalanceQueryPort balances;

    public ClaimAdjustmentService(ClaimLedgerPort ledger, BalanceQueryPort balances) {
        this.ledger = ledger;
        this.balances = balances;
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
        ClaimContext context = new ClaimContext(
                command.discrepancyId(), subcontractorId, shipmentId, command.decidedAt(), command.decidedBy());
        YarnDistribution.proportional(Kilograms.round(command.claimedKg()), dispatched).stream()
                .map(share -> new ClaimShare(share.yarnType(), share.netKg()))
                .map(share -> BalanceMovement.claimAdjustment(share, context))
                .forEach(ledger::append);
    }
}
