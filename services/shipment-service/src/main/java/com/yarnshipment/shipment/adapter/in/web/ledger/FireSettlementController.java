package com.yarnshipment.shipment.adapter.in.web.ledger;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.ledger.FireViews.FireSettlementPageView;
import com.yarnshipment.shipment.adapter.in.web.ledger.FireViews.FireSettlementView;
import com.yarnshipment.shipment.application.port.in.fire.FireSettlementQuery;
import com.yarnshipment.shipment.application.port.in.fire.GetFireSettlementUseCase;
import com.yarnshipment.shipment.application.port.in.fire.ListFireSettlementsQuery;
import com.yarnshipment.shipment.application.port.in.fire.ListFireSettlementsUseCase;
import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import com.yarnshipment.shipment.domain.fire.FireSettlementStatus;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Fire dosyasını okuma uçları (analiz 5.10). Method security proxy'si gerektirdiği için final değildir. */
@RestController
public class FireSettlementController {

    private final GetFireSettlementUseCase getFireSettlementUseCase;
    private final ListFireSettlementsUseCase listFireSettlementsUseCase;
    private final CurrentActorProvider currentActorProvider;

    public FireSettlementController(
            GetFireSettlementUseCase getFireSettlementUseCase,
            ListFireSettlementsUseCase listFireSettlementsUseCase,
            CurrentActorProvider currentActorProvider) {
        this.getFireSettlementUseCase = getFireSettlementUseCase;
        this.listFireSettlementsUseCase = listFireSettlementsUseCase;
        this.currentActorProvider = currentActorProvider;
    }

    @GetMapping("/api/v1/shipments/{shipmentId}/fire-settlement")
    @PreAuthorize("hasAuthority('SHIPMENT_VIEW')")
    public ResponseEntity<FireSettlementView> getFireSettlement(@PathVariable UUID shipmentId) {
        FireSettlementQuery query = new FireSettlementQuery(currentActorProvider.getCurrentActor(), shipmentId);
        return ResponseEntity.ok(FireViews.fireSettlement(getFireSettlementUseCase.getFireSettlement(query)));
    }

    @GetMapping("/api/v1/fire-settlements")
    @PreAuthorize("hasAuthority('BALANCE_VIEW')")
    public ResponseEntity<FireSettlementPageView> listFireSettlements(
            @RequestParam(required = false) @Nullable String status,
            @RequestParam(required = false) @Nullable UUID subcontractorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ListFireSettlementsQuery query = new ListFireSettlementsQuery(
                currentActorProvider.getCurrentActor(), statusOf(status), subcontractorId, page, size);
        return ResponseEntity.ok(FireViews.fireSettlements(listFireSettlementsUseCase.listFireSettlements(query)));
    }

    private static @Nullable FireSettlementStatus statusOf(@Nullable String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return FireSettlementStatus.valueOf(status.strip());
        } catch (IllegalArgumentException e) {
            throw new InvalidLedgerInputException("status", e);
        }
    }
}
