package com.yarnshipment.shipment.adapter.in.web.ledger;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.ledger.FabricPlanViews.FabricDrawView;
import com.yarnshipment.shipment.adapter.in.web.ledger.FabricPlanViews.FabricPreviewView;
import com.yarnshipment.shipment.adapter.in.web.ledger.FabricPlanViews.YarnOptionView;
import com.yarnshipment.shipment.adapter.in.web.ledger.LedgerRequests.FabricPreviewRequest;
import com.yarnshipment.shipment.application.port.in.fabric.ListFabricDrawsUseCase;
import com.yarnshipment.shipment.application.port.in.fabric.ListFabricYarnOptionsUseCase;
import com.yarnshipment.shipment.application.port.in.fabric.PreviewFabricReceiptCommand;
import com.yarnshipment.shipment.application.port.in.fabric.PreviewFabricReceiptUseCase;
import com.yarnshipment.shipment.application.port.in.fabric.ShipmentRecordsQuery;
import com.yarnshipment.shipment.application.port.in.stock.RequireWorkAreaUseCase;
import com.yarnshipment.shipment.domain.stock.WorkArea;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kumaş girişinin yardımcı uçları: fasoncunun elindeki iplikler, kaydetmeden önce düşüm önizlemesi ve sevkiyatın
 * ipliğinden üretilen kumaşlar. Method security proxy'si gerektirdiği için final değildir.
 */
@RestController
@RequestMapping("/api/v1/shipments/{shipmentId}/fabric-receipts")
public class FabricYarnController {

    private final PreviewFabricReceiptUseCase previewFabricReceiptUseCase;
    private final ListFabricYarnOptionsUseCase listFabricYarnOptionsUseCase;
    private final ListFabricDrawsUseCase listFabricDrawsUseCase;
    private final CurrentActorProvider currentActorProvider;
    private final RequireWorkAreaUseCase workAreas;

    public FabricYarnController(
            PreviewFabricReceiptUseCase previewFabricReceiptUseCase,
            ListFabricYarnOptionsUseCase listFabricYarnOptionsUseCase,
            ListFabricDrawsUseCase listFabricDrawsUseCase,
            CurrentActorProvider currentActorProvider,
            RequireWorkAreaUseCase workAreas) {
        this.previewFabricReceiptUseCase = previewFabricReceiptUseCase;
        this.listFabricYarnOptionsUseCase = listFabricYarnOptionsUseCase;
        this.listFabricDrawsUseCase = listFabricDrawsUseCase;
        this.currentActorProvider = currentActorProvider;
        this.workAreas = workAreas;
    }

    /** Fasoncunun elindeki iplikler (tüm açık sevkiyatlarından), kumaş girerken oran yazılacak satırlar. */
    @GetMapping("/yarn-options")
    @PreAuthorize("hasAuthority('FABRIC_RECEIPT_RECORD')")
    public ResponseEntity<List<YarnOptionView>> listYarnOptions(@PathVariable UUID shipmentId) {
        workAreas.requireAccess(currentActorProvider.getCurrentActor(), WorkArea.SUBCONTRACTOR);
        ShipmentRecordsQuery query = new ShipmentRecordsQuery(currentActorProvider.getCurrentActor(), shipmentId);
        return ResponseEntity.ok(listFabricYarnOptionsUseCase.listFabricYarnOptions(query).stream()
                .map(FabricPlanViews::option)
                .toList());
    }

    /** Kaydetmeden önce her ipliğin kaç kg, hangi sevkiyattan düşeceğini gösterir; hiçbir şey yazmaz. */
    @PostMapping("/preview")
    @PreAuthorize("hasAuthority('FABRIC_RECEIPT_RECORD')")
    public ResponseEntity<FabricPreviewView> previewFabricReceipt(
            @PathVariable UUID shipmentId, @RequestBody FabricPreviewRequest request) {
        workAreas.requireAccess(currentActorProvider.getCurrentActor(), WorkArea.SUBCONTRACTOR);
        PreviewFabricReceiptCommand command = new PreviewFabricReceiptCommand(
                currentActorProvider.getCurrentActor(),
                shipmentId,
                LedgerRequests.kg(request.netKg(), "netKg"),
                FabricReceiptController.ratios(request.yarns()));
        return ResponseEntity.ok(FabricPlanViews.preview(previewFabricReceiptUseCase.previewFabricReceipt(command)));
    }

    /** Bu sevkiyatın ipliğinden üretilen kumaşlar (başka sevkiyatta girilenler dahil). */
    @GetMapping("/drawn-from-yarn")
    @PreAuthorize("hasAuthority('SHIPMENT_VIEW')")
    public ResponseEntity<List<FabricDrawView>> listFabricDraws(@PathVariable UUID shipmentId) {
        workAreas.requireAccess(currentActorProvider.getCurrentActor(), WorkArea.SUBCONTRACTOR);
        ShipmentRecordsQuery query = new ShipmentRecordsQuery(currentActorProvider.getCurrentActor(), shipmentId);
        return ResponseEntity.ok(listFabricDrawsUseCase.listFabricDraws(query).stream()
                .map(FabricPlanViews::drawEntry)
                .toList());
    }
}
