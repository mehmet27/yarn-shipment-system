package com.yarnshipment.shipment.application.service;

import com.yarnshipment.shipment.application.port.in.claim.RecordClaimAdjustmentCommand;
import com.yarnshipment.shipment.application.port.in.claim.RecordClaimAdjustmentUseCase;
import com.yarnshipment.shipment.application.port.out.ledger.ClaimLedgerPort;
import com.yarnshipment.shipment.application.port.out.ledger.ClaimLedgerPort.InvoiceableClaim;
import com.yarnshipment.shipment.application.port.out.ledger.FireSettlementPort;
import com.yarnshipment.shipment.domain.balance.Kilograms;
import com.yarnshipment.shipment.domain.fire.FireSettlement;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Haklı kalite itirazının kilosunu fire dosyasına faturalanacak kilo olarak yazar (KR-15, analiz 5.12). Kilo bakiyeden
 * hemen düşülmez; müdür fatura kesince düşer. İtiraz için süre sınırı olmadığından kapanmış dosyaya gelen itiraz da
 * dosyayı yeniden faturalanacak yapar. Fire hesabı itiraz kilosunu fire tabanından düşer. Aynı uyuşmazlığın olayı
 * tekrar gelirse kayıt değişmez, kapanmış dosya da yeniden açılmaz; fire dosyası olmayan (fasoncusuz) sevkiyatta
 * yapılacak bir şey yoktur.
 */
@Service
public class ClaimAdjustmentService implements RecordClaimAdjustmentUseCase {

    private final ClaimLedgerPort ledger;
    private final FireSettlementPort settlements;

    public ClaimAdjustmentService(ClaimLedgerPort ledger, FireSettlementPort settlements) {
        this.ledger = ledger;
        this.settlements = settlements;
    }

    @Override
    @Transactional
    public void recordClaimAdjustment(RecordClaimAdjustmentCommand command) {
        settlements.lock(command.shipmentId()).ifPresent(file -> record(file, command));
    }

    private void record(FireSettlement file, RecordClaimAdjustmentCommand command) {
        boolean recorded = ledger.recordInvoiceableClaim(new InvoiceableClaim(
                command.discrepancyId(),
                command.shipmentId(),
                Kilograms.round(command.claimedKg()),
                command.decidedAt(),
                command.decidedBy()));
        if (!recorded) {
            return;
        }
        FireSettlement reopened = file.reopenedForClaim();
        if (!reopened.equals(file)) {
            settlements.save(reopened, command.decidedAt());
        }
    }
}
