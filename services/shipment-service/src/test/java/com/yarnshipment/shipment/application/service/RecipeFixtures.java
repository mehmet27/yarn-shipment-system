package com.yarnshipment.shipment.application.service;

import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.domain.model.RecipeLine;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentParties;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Recipe and party fixtures shared by service tests. */
final class RecipeFixtures {

    private RecipeFixtures() {}

    static RecipeLine recipeLine(int lineNo, @Nullable UUID yarnId, @Nullable String name, BigDecimal netKg) {
        return new RecipeLine(lineNo, yarnId, name, null, netKg);
    }

    static ShipmentParties parties(String title, @Nullable UUID driverId, @Nullable String plate) {
        return new ShipmentParties(TestShipments.SUBCONTRACTOR_ID, title, driverId, null, plate);
    }

    static Shipment mismatched() {
        Shipment base = TestShipments.shipment(ShipmentStatus.WEIGHT_MISMATCH);
        return new Shipment(
                base.id(),
                base.shipmentNo(),
                base.siteId(),
                base.status(),
                base.parties(),
                base.planned(),
                base.dispatchWeighing(),
                null,
                base.createdAt(),
                new Shipment.RecipeState(
                        List.of(
                                recipeLine(1, null, "VİSKON", ServiceTestSupport.decimal("1000.000")),
                                recipeLine(2, null, "POLY", ServiceTestSupport.decimal("982.500"))),
                        ServiceTestSupport.decimal("0.0400"),
                        null));
    }
}
