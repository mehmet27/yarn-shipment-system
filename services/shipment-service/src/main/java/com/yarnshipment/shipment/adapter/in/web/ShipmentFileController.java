package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.api.ShipmentFileApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.ShipmentBagsResponse;
import com.yarnshipment.shipment.adapter.in.web.api.model.ShipmentHistoryResponse;
import com.yarnshipment.shipment.adapter.in.web.api.model.StatusChangeView;
import com.yarnshipment.shipment.application.port.in.GetShipmentBagsQuery;
import com.yarnshipment.shipment.application.port.in.GetShipmentBagsUseCase;
import com.yarnshipment.shipment.application.port.in.GetShipmentHistoryQuery;
import com.yarnshipment.shipment.application.port.in.GetShipmentHistoryUseCase;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sevkiyat dosyası sekmeleri: durum geçmişi ve çuvallar. Method security proxy'si gerektirdiği için final değildir
 * (SEC-02).
 */
@RestController
public class ShipmentFileController implements ShipmentFileApi {

    private final GetShipmentHistoryUseCase getShipmentHistoryUseCase;
    private final GetShipmentBagsUseCase getShipmentBagsUseCase;
    private final CurrentActorProvider currentActorProvider;

    public ShipmentFileController(
            GetShipmentHistoryUseCase getShipmentHistoryUseCase,
            GetShipmentBagsUseCase getShipmentBagsUseCase,
            CurrentActorProvider currentActorProvider) {
        this.getShipmentHistoryUseCase = getShipmentHistoryUseCase;
        this.getShipmentBagsUseCase = getShipmentBagsUseCase;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('SHIPMENT_VIEW')")
    public ResponseEntity<ShipmentHistoryResponse> getShipmentHistory(UUID shipmentId) {
        GetShipmentHistoryQuery query = new GetShipmentHistoryQuery(currentActorProvider.getCurrentActor(), shipmentId);
        return ResponseEntity.ok(new ShipmentHistoryResponse(getShipmentHistoryUseCase.getHistory(query).stream()
                .map(change -> new StatusChangeView(
                                WebMappings.status(change.to()),
                                WebMappings.utc(change.changedAt()),
                                change.changedBy())
                        .fromStatus(change.from() == null ? null : WebMappings.status(change.from()))
                        .reason(change.reason()))
                .toList()));
    }

    @Override
    @PreAuthorize("hasAuthority('SHIPMENT_VIEW')")
    public ResponseEntity<ShipmentBagsResponse> getShipmentBags(UUID shipmentId) {
        GetShipmentBagsQuery query = new GetShipmentBagsQuery(currentActorProvider.getCurrentActor(), shipmentId);
        return ResponseEntity.ok(ShipmentBagViews.toResponse(getShipmentBagsUseCase.getBags(query)));
    }
}
