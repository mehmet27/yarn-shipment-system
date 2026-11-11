package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.adapter.in.web.api.model.CreateShipmentRequest;
import com.yarnshipment.shipment.adapter.in.web.api.model.RecipeLineInput;
import com.yarnshipment.shipment.application.port.in.CreateShipmentCommand;
import com.yarnshipment.shipment.domain.model.RecipeLine;
import com.yarnshipment.shipment.domain.model.ShipmentParties;
import com.yarnshipment.shipment.domain.model.WidthType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Sevkiyat oluşturma isteğini uygulama komutuna çevirir. */
final class ShipmentCreations {

    private ShipmentCreations() {}

    static CreateShipmentCommand toCommand(Actor actor, UUID idempotencyKey, CreateShipmentRequest request) {
        ShipmentParties parties = new ShipmentParties(
                request.getSubcontractorId(),
                request.getSubcontractorTitle(),
                request.getDriverId(),
                request.getDriverName(),
                request.getPlateNo());
        List<RecipeLine> lines = new ArrayList<>();
        for (RecipeLineInput input : request.getLines()) {
            lines.add(new RecipeLine(
                    lines.size() + 1,
                    input.getYarnTypeId(),
                    input.getYarnTypeText(),
                    input.getLotId(),
                    new BigDecimal(input.getNetKg()),
                    input.getFabricType(),
                    input.getLycra(),
                    input.getWidthType() == null ? null : WidthType.valueOf(input.getWidthType()),
                    input.getYarnLength(),
                    input.getPussFine(),
                    input.getUnitPrice() == null ? null : new BigDecimal(input.getUnitPrice())));
        }
        BigDecimal fireRate = request.getFireRate() == null ? null : new BigDecimal(request.getFireRate());
        return new CreateShipmentCommand(
                actor, idempotencyKey, request.getSiteId(), parties, request.getVehicleId(), lines, fireRate);
    }
}
