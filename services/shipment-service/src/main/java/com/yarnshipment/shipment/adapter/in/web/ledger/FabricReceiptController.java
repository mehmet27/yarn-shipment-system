package com.yarnshipment.shipment.adapter.in.web.ledger;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.ledger.FabricViews.FabricReceiptView;
import com.yarnshipment.shipment.adapter.in.web.ledger.LedgerRequests.FabricReceiptRequest;
import com.yarnshipment.shipment.adapter.in.web.ledger.LedgerRequests.YarnShareRequest;
import com.yarnshipment.shipment.application.port.in.fabric.ListFabricReceiptsUseCase;
import com.yarnshipment.shipment.application.port.in.fabric.RecordFabricReceiptCommand;
import com.yarnshipment.shipment.application.port.in.fabric.RecordFabricReceiptUseCase;
import com.yarnshipment.shipment.application.port.in.fabric.ShipmentRecordsQuery;
import com.yarnshipment.shipment.domain.fabric.YarnShare;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sevkiyatın kumaş girişleri (analiz 5.8). Method security proxy'si gerektirdiği için final değildir. */
@RestController
@RequestMapping("/api/v1/shipments/{shipmentId}/fabric-receipts")
public class FabricReceiptController {

    private final ListFabricReceiptsUseCase listFabricReceiptsUseCase;
    private final RecordFabricReceiptUseCase recordFabricReceiptUseCase;
    private final CurrentActorProvider currentActorProvider;

    public FabricReceiptController(
            ListFabricReceiptsUseCase listFabricReceiptsUseCase,
            RecordFabricReceiptUseCase recordFabricReceiptUseCase,
            CurrentActorProvider currentActorProvider) {
        this.listFabricReceiptsUseCase = listFabricReceiptsUseCase;
        this.recordFabricReceiptUseCase = recordFabricReceiptUseCase;
        this.currentActorProvider = currentActorProvider;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SHIPMENT_VIEW')")
    public ResponseEntity<List<FabricReceiptView>> listFabricReceipts(@PathVariable UUID shipmentId) {
        ShipmentRecordsQuery query = new ShipmentRecordsQuery(currentActorProvider.getCurrentActor(), shipmentId);
        return ResponseEntity.ok(listFabricReceiptsUseCase.listFabricReceipts(query).stream()
                .map(FabricViews::fabricReceipt)
                .toList());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FABRIC_RECEIPT_RECORD')")
    public ResponseEntity<FabricReceiptView> recordFabricReceipt(
            @PathVariable UUID shipmentId, @RequestBody FabricReceiptRequest request) {
        List<YarnShare> yarns = request.yarns() == null
                ? null
                : request.yarns().stream().map(FabricReceiptController::toShare).toList();
        RecordFabricReceiptCommand command = new RecordFabricReceiptCommand(
                currentActorProvider.getCurrentActor(),
                shipmentId,
                LedgerRequests.required(request.fabricType(), "fabricType"),
                LedgerRequests.kg(request.netKg(), "netKg"),
                request.rollCount(),
                LedgerRequests.required(request.waybillNo(), "waybillNo"),
                request.receivedAt(),
                yarns);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(FabricViews.fabricReceipt(recordFabricReceiptUseCase.recordFabricReceipt(command)));
    }

    private static YarnShare toShare(YarnShareRequest request) {
        return new YarnShare(
                LedgerRequests.required(request.yarnType(), "yarns.yarnType"),
                LedgerRequests.kg(request.netKg(), "yarns.netKg"));
    }
}
