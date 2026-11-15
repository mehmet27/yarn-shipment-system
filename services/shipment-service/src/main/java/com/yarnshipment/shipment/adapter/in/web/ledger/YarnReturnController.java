package com.yarnshipment.shipment.adapter.in.web.ledger;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.ledger.FabricViews.YarnReturnView;
import com.yarnshipment.shipment.adapter.in.web.ledger.LedgerRequests.YarnReturnRequest;
import com.yarnshipment.shipment.application.port.in.fabric.ListYarnReturnsUseCase;
import com.yarnshipment.shipment.application.port.in.fabric.RecordYarnReturnCommand;
import com.yarnshipment.shipment.application.port.in.fabric.RecordYarnReturnUseCase;
import com.yarnshipment.shipment.application.port.in.fabric.ShipmentRecordsQuery;
import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Fasoncudan depoya dönen iplik (analiz 5.9). Method security proxy'si gerektirdiği için final değildir. */
@RestController
@RequestMapping("/api/v1/shipments/{shipmentId}/yarn-returns")
public class YarnReturnController {

    private final ListYarnReturnsUseCase listYarnReturnsUseCase;
    private final RecordYarnReturnUseCase recordYarnReturnUseCase;
    private final CurrentActorProvider currentActorProvider;

    public YarnReturnController(
            ListYarnReturnsUseCase listYarnReturnsUseCase,
            RecordYarnReturnUseCase recordYarnReturnUseCase,
            CurrentActorProvider currentActorProvider) {
        this.listYarnReturnsUseCase = listYarnReturnsUseCase;
        this.recordYarnReturnUseCase = recordYarnReturnUseCase;
        this.currentActorProvider = currentActorProvider;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SHIPMENT_VIEW')")
    public ResponseEntity<List<YarnReturnView>> listYarnReturns(@PathVariable UUID shipmentId) {
        ShipmentRecordsQuery query = new ShipmentRecordsQuery(currentActorProvider.getCurrentActor(), shipmentId);
        return ResponseEntity.ok(listYarnReturnsUseCase.listYarnReturns(query).stream()
                .map(FabricViews::yarnReturn)
                .toList());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('YARN_RETURN_RECORD')")
    public ResponseEntity<YarnReturnView> recordYarnReturn(
            @PathVariable UUID shipmentId, @RequestBody YarnReturnRequest request) {
        requireReturnDirection(request.direction());
        RecordYarnReturnCommand command = new RecordYarnReturnCommand(
                currentActorProvider.getCurrentActor(),
                shipmentId,
                LedgerRequests.required(request.yarnType(), "yarnType"),
                request.lotId(),
                LedgerRequests.kg(request.netKg(), "netKg"),
                request.waybillNo());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(FabricViews.yarnReturn(recordYarnReturnUseCase.recordYarnReturn(command)));
    }

    /**
     * Yön alanı kaldırıldı; yalnızca fasoncudan depoya dönüş vardır. Eski bir istemci "fasoncudan gelen iplik"
     * gönderirse bakiyeyi sessizce yanlış yazmamak için reddedilir.
     */
    private static void requireReturnDirection(@Nullable String value) {
        if (value != null && !"RETURN".equals(value.strip())) {
            throw new InvalidLedgerInputException("direction");
        }
    }
}
