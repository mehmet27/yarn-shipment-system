package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.api.ShipmentsApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.PageMetadata;
import com.yarnshipment.shipment.adapter.in.web.api.model.ShipmentDetailResponse;
import com.yarnshipment.shipment.adapter.in.web.api.model.ShipmentPageResponse;
import com.yarnshipment.shipment.adapter.in.web.api.model.ShipmentStatus;
import com.yarnshipment.shipment.adapter.in.web.api.model.ShipmentSummaryView;
import com.yarnshipment.shipment.application.port.in.GetShipmentQuery;
import com.yarnshipment.shipment.application.port.in.GetShipmentUseCase;
import com.yarnshipment.shipment.application.port.in.ListShipmentsQuery;
import com.yarnshipment.shipment.application.port.in.ListShipmentsUseCase;
import com.yarnshipment.shipment.application.port.in.ShipmentListItem;
import com.yarnshipment.shipment.application.port.in.ShipmentPage;
import com.yarnshipment.shipment.domain.model.Shipment;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Sevkiyat okuma uçları. Method security proxy'si gerektirdiği için final değildir (SEC-02). */
@RestController
public class ShipmentController implements ShipmentsApi {

    private final ListShipmentsUseCase listShipmentsUseCase;
    private final GetShipmentUseCase getShipmentUseCase;
    private final CurrentActorProvider currentActorProvider;

    public ShipmentController(
            ListShipmentsUseCase listShipmentsUseCase,
            GetShipmentUseCase getShipmentUseCase,
            CurrentActorProvider currentActorProvider) {
        this.listShipmentsUseCase = listShipmentsUseCase;
        this.getShipmentUseCase = getShipmentUseCase;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('SHIPMENT_VIEW')")
    public ResponseEntity<ShipmentPageResponse> listShipments(
            Integer page, Integer size, @Nullable List<ShipmentStatus> status) {
        ListShipmentsQuery query =
                new ListShipmentsQuery(currentActorProvider.getCurrentActor(), statuses(status), page, size);
        return ResponseEntity.ok(toResponse(listShipmentsUseCase.listShipments(query)));
    }

    @Override
    @PreAuthorize("hasAuthority('SHIPMENT_VIEW')")
    public ResponseEntity<ShipmentDetailResponse> getShipment(UUID shipmentId) {
        GetShipmentQuery query = new GetShipmentQuery(currentActorProvider.getCurrentActor(), shipmentId);
        return ResponseEntity.ok(ShipmentViews.toDetail(getShipmentUseCase.getShipment(query)));
    }

    private static Set<com.yarnshipment.shipment.domain.model.ShipmentStatus> statuses(
            @Nullable List<ShipmentStatus> status) {
        if (status == null) {
            return Set.of();
        }
        return status.stream()
                .map(value -> com.yarnshipment.shipment.domain.model.ShipmentStatus.valueOf(value.name()))
                .collect(Collectors.toSet());
    }

    private static ShipmentPageResponse toResponse(ShipmentPage page) {
        PageMetadata metadata = new PageMetadata(page.number(), page.size(), page.totalElements(), page.totalPages());
        return new ShipmentPageResponse(
                page.items().stream().map(ShipmentController::toSummary).toList(), metadata);
    }

    private static ShipmentSummaryView toSummary(ShipmentListItem item) {
        Shipment shipment = item.shipment();
        return new ShipmentSummaryView()
                .shipmentId(shipment.id())
                .shipmentNo(shipment.shipmentNo())
                .siteId(shipment.siteId())
                .status(WebMappings.status(shipment.status()))
                .subcontractorId(shipment.subcontractorId())
                .subcontractorTitle(shipment.parties().subcontractorTitle())
                .driverName(shipment.parties().driverName())
                .plateNo(shipment.parties().plateNo())
                .createdAt(WebMappings.utc(shipment.createdAt()))
                .allowedActions(WebMappings.actions(item.allowedActions()));
    }
}
