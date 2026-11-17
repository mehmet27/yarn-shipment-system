package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.adapter.in.web.api.model.CreateShipmentRequest;
import com.yarnshipment.shipment.adapter.in.web.api.model.RecipeLineInput;
import com.yarnshipment.shipment.application.port.in.CreateShipmentCommand;
import com.yarnshipment.shipment.domain.model.InvalidNewShipmentException;
import com.yarnshipment.shipment.domain.model.OrderSpec;
import com.yarnshipment.shipment.domain.model.RecipeLine;
import com.yarnshipment.shipment.domain.model.ShipmentParties;
import com.yarnshipment.shipment.domain.model.WidthType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Sevkiyat oluşturma isteğini uygulama komutuna çevirir. */
final class ShipmentCreations {

    private ShipmentCreations() {}

    static CreateShipmentCommand toCommand(Actor actor, UUID idempotencyKey, CreateShipmentRequest request) {
        ShipmentParties parties = parties(request);
        List<RecipeLine> lines = lines(request.getLines());
        BigDecimal fireRate = request.getFireRate() == null ? null : new BigDecimal(request.getFireRate());
        return new CreateShipmentCommand(
                actor,
                idempotencyKey,
                request.getSiteId(),
                parties,
                request.getVehicleId(),
                lines,
                fireRate,
                request.getShipmentDate());
    }

    /** Reçete satırı girdilerini sıraya göre numaralı satırlara çevirir. */
    static List<RecipeLine> lines(List<RecipeLineInput> inputs) {
        List<RecipeLine> lines = new ArrayList<>();
        for (RecipeLineInput input : inputs) {
            lines.add(new RecipeLine(
                    lines.size() + 1,
                    input.getYarnTypeId(),
                    input.getYarnTypeText(),
                    input.getLotId(),
                    new BigDecimal(input.getNetKg()),
                    orderSpec(input)));
        }
        return lines;
    }

    private static ShipmentParties parties(CreateShipmentRequest request) {
        return new ShipmentParties(
                request.getSubcontractorId(),
                request.getSubcontractorTitle(),
                request.getDriverId(),
                request.getDriverName(),
                request.getPlateNo(),
                request.getSourceSubcontractorId(),
                request.getSourceSubcontractorTitle(),
                request.getDirectTransferSupplier(),
                request.getDirectTransferWaybillNo());
    }

    private static OrderSpec orderSpec(RecipeLineInput input) {
        return new OrderSpec(
                input.getFabricType(),
                input.getLycra(),
                input.getWidthType() == null ? null : WidthType.valueOf(input.getWidthType()),
                input.getYarnLength(),
                input.getPussFine(),
                requiredPrice(input.getUnitPrice()));
    }

    /** Yeni sevkiyatta birim fiyat zorunludur (genel müdürün tutar raporu bu fiyattan hesaplanır). */
    private static BigDecimal requiredPrice(@Nullable String unitPrice) {
        if (unitPrice == null || unitPrice.isBlank()) {
            throw new InvalidNewShipmentException("lines", "unit price required");
        }
        try {
            return new BigDecimal(unitPrice.strip());
        } catch (NumberFormatException e) {
            throw new InvalidNewShipmentException("lines", "unit price invalid");
        }
    }
}
