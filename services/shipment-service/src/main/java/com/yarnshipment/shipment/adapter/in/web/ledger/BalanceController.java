package com.yarnshipment.shipment.adapter.in.web.ledger;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.ledger.BalanceViews.BalanceSummaryView;
import com.yarnshipment.shipment.adapter.in.web.ledger.BalanceViews.MovementPageView;
import com.yarnshipment.shipment.application.port.in.balance.GetBalancesQuery;
import com.yarnshipment.shipment.application.port.in.balance.GetBalancesUseCase;
import com.yarnshipment.shipment.application.port.in.balance.ListBalanceMovementsUseCase;
import com.yarnshipment.shipment.application.port.in.balance.ListMovementsQuery;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Fasoncu bakiyesi ve hareket defteri (analiz 5.7). Method security proxy'si gerektirdiği için final değildir. */
@RestController
@RequestMapping("/api/v1/balances")
public class BalanceController {

    private final GetBalancesUseCase getBalancesUseCase;
    private final ListBalanceMovementsUseCase listMovementsUseCase;
    private final CurrentActorProvider currentActorProvider;

    public BalanceController(
            GetBalancesUseCase getBalancesUseCase,
            ListBalanceMovementsUseCase listMovementsUseCase,
            CurrentActorProvider currentActorProvider) {
        this.getBalancesUseCase = getBalancesUseCase;
        this.listMovementsUseCase = listMovementsUseCase;
        this.currentActorProvider = currentActorProvider;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('BALANCE_VIEW')")
    public ResponseEntity<BalanceSummaryView> getBalances(
            @RequestParam(required = false) @Nullable UUID subcontractorId) {
        GetBalancesQuery query = new GetBalancesQuery(currentActorProvider.getCurrentActor(), subcontractorId);
        return ResponseEntity.ok(BalanceViews.balances(getBalancesUseCase.getBalances(query)));
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAuthority('BALANCE_VIEW')")
    public ResponseEntity<MovementPageView> listMovements(
            @RequestParam(required = false) @Nullable UUID subcontractorId,
            @RequestParam(required = false) @Nullable String yarnType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ListMovementsQuery query =
                new ListMovementsQuery(currentActorProvider.getCurrentActor(), subcontractorId, yarnType, page, size);
        return ResponseEntity.ok(BalanceViews.movements(listMovementsUseCase.listBalanceMovements(query)));
    }
}
