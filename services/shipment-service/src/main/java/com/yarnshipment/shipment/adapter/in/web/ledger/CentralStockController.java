package com.yarnshipment.shipment.adapter.in.web.ledger;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.ledger.CentralStockViews.CentralProductionView;
import com.yarnshipment.shipment.adapter.in.web.ledger.CentralStockViews.CentralStockMovementView;
import com.yarnshipment.shipment.adapter.in.web.ledger.CentralStockViews.CentralStockView;
import com.yarnshipment.shipment.adapter.in.web.ledger.LedgerRequests.CentralProductionRequest;
import com.yarnshipment.shipment.adapter.in.web.ledger.LedgerRequests.YarnReceiptRequest;
import com.yarnshipment.shipment.application.port.in.stock.GetCentralStockUseCase;
import com.yarnshipment.shipment.application.port.in.stock.RecordCentralProductionUseCase;
import com.yarnshipment.shipment.application.port.in.stock.RecordYarnReceiptUseCase;
import com.yarnshipment.shipment.domain.fabric.YarnShare;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Merkez iplik stoku, merkeze iplik girişi ve merkez fabrika kumaş girişi. Method security için final değildir. */
@RestController
@RequestMapping("/api/v1/central-stock")
public class CentralStockController {

    private final GetCentralStockUseCase stockUseCase;
    private final RecordYarnReceiptUseCase recordReceipt;
    private final RecordCentralProductionUseCase recordProduction;
    private final CurrentActorProvider currentActorProvider;

    public CentralStockController(
            GetCentralStockUseCase getStock,
            RecordYarnReceiptUseCase recordReceipt,
            RecordCentralProductionUseCase recordProduction,
            CurrentActorProvider currentActorProvider) {
        this.stockUseCase = getStock;
        this.recordReceipt = recordReceipt;
        this.recordProduction = recordProduction;
        this.currentActorProvider = currentActorProvider;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CENTRAL_STOCK_VIEW')")
    public ResponseEntity<CentralStockView> getStock() {
        return ResponseEntity.ok(
                CentralStockViews.stock(stockUseCase.getCentralStock(currentActorProvider.getCurrentActor())));
    }

    @PostMapping("/yarn-receipts")
    @PreAuthorize("hasAuthority('CENTRAL_STOCK_RECORD')")
    public ResponseEntity<CentralStockMovementView> recordYarnReceipt(@RequestBody YarnReceiptRequest request) {
        RecordYarnReceiptUseCase.Command command = new RecordYarnReceiptUseCase.Command(
                currentActorProvider.getCurrentActor(),
                LedgerRequests.required(request.yarnType(), "yarnType"),
                LedgerRequests.kg(request.netKg(), "netKg"),
                request.note());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CentralStockViews.movement(recordReceipt.recordYarnReceipt(command)));
    }

    @PostMapping("/productions")
    @PreAuthorize("hasAuthority('CENTRAL_PRODUCTION_RECORD')")
    public ResponseEntity<CentralProductionView> recordProduction(@RequestBody CentralProductionRequest request) {
        List<YarnShare> yarns = request.yarns() == null
                ? List.of()
                : request.yarns().stream()
                        .map(share -> new YarnShare(
                                LedgerRequests.required(share.yarnType(), "yarns.yarnType"),
                                LedgerRequests.kg(share.netKg(), "yarns.netKg")))
                        .toList();
        RecordCentralProductionUseCase.Command command = new RecordCentralProductionUseCase.Command(
                currentActorProvider.getCurrentActor(),
                LedgerRequests.required(request.fabricType(), "fabricType"),
                LedgerRequests.rate(request.fireRate()),
                request.rollCount(),
                request.waybillNo(),
                yarns);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CentralStockViews.production(recordProduction.recordCentralProduction(command)));
    }
}
