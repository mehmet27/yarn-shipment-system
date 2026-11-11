package com.yarnshipment.shipment.adapter.in.web.ledger;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.ledger.FireViews.FireSettlementView;
import com.yarnshipment.shipment.adapter.in.web.ledger.LedgerRequests.FireRateRequest;
import com.yarnshipment.shipment.adapter.in.web.ledger.LedgerRequests.InvoiceRequest;
import com.yarnshipment.shipment.application.port.in.fire.ChangeFireRateCommand;
import com.yarnshipment.shipment.application.port.in.fire.ChangeFireRateUseCase;
import com.yarnshipment.shipment.application.port.in.fire.CloseFireSettlementUseCase;
import com.yarnshipment.shipment.application.port.in.fire.FireSettlementQuery;
import com.yarnshipment.shipment.application.port.in.fire.MarkInvoicedCommand;
import com.yarnshipment.shipment.application.port.in.fire.MarkInvoicedUseCase;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Fire oranı, "fatura edildi" ve kapatma uçları (analiz 5.11). Method security proxy'si için final değildir. */
@RestController
public class FireSettlementActionController {

    private final ChangeFireRateUseCase changeFireRateUseCase;
    private final MarkInvoicedUseCase markInvoicedUseCase;
    private final CloseFireSettlementUseCase closeFireSettlementUseCase;
    private final CurrentActorProvider currentActorProvider;

    public FireSettlementActionController(
            ChangeFireRateUseCase changeFireRateUseCase,
            MarkInvoicedUseCase markInvoicedUseCase,
            CloseFireSettlementUseCase closeFireSettlementUseCase,
            CurrentActorProvider currentActorProvider) {
        this.changeFireRateUseCase = changeFireRateUseCase;
        this.markInvoicedUseCase = markInvoicedUseCase;
        this.closeFireSettlementUseCase = closeFireSettlementUseCase;
        this.currentActorProvider = currentActorProvider;
    }

    @PatchMapping("/api/v1/shipments/{shipmentId}/fire-rate")
    @PreAuthorize("hasAuthority('FIRE_RATE_EDIT')")
    public ResponseEntity<FireSettlementView> changeFireRate(
            @PathVariable UUID shipmentId, @RequestBody FireRateRequest request) {
        ChangeFireRateCommand command = new ChangeFireRateCommand(
                currentActorProvider.getCurrentActor(),
                shipmentId,
                LedgerRequests.rate(request.fireRate()),
                LedgerRequests.required(request.reason(), "reason"));
        return ResponseEntity.ok(FireViews.fireSettlement(changeFireRateUseCase.changeFireRate(command)));
    }

    @PostMapping("/api/v1/shipments/{shipmentId}/fire-settlement/invoice")
    @PreAuthorize("hasAuthority('INVOICE_MARK')")
    public ResponseEntity<FireSettlementView> markInvoiced(
            @PathVariable UUID shipmentId, @RequestBody InvoiceRequest request) {
        MarkInvoicedCommand command = new MarkInvoicedCommand(
                currentActorProvider.getCurrentActor(),
                shipmentId,
                LedgerRequests.required(request.invoiceNo(), "invoiceNo"),
                LedgerRequests.requiredDate(request.invoicedOn(), "invoicedOn"),
                request.note());
        return ResponseEntity.ok(FireViews.fireSettlement(markInvoicedUseCase.markInvoiced(command)));
    }

    @PostMapping("/api/v1/shipments/{shipmentId}/fire-settlement/close")
    @PreAuthorize("hasAuthority('INVOICE_MARK')")
    public ResponseEntity<FireSettlementView> closeFireSettlement(@PathVariable UUID shipmentId) {
        FireSettlementQuery query = new FireSettlementQuery(currentActorProvider.getCurrentActor(), shipmentId);
        return ResponseEntity.ok(FireViews.fireSettlement(closeFireSettlementUseCase.closeFireSettlement(query)));
    }
}
