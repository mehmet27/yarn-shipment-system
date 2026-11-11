package com.yarnshipment.shipment;

import com.yarnshipment.shipment.domain.model.PlannedLoad;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentParties;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import com.yarnshipment.shipment.domain.model.Weighing;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Testlerde kullanılan sevkiyat örnekleri. */
public final class TestShipments {

    public static final UUID SITE_ID = TestTokens.SITE_ID;
    public static final UUID SUBCONTRACTOR_ID = TestTokens.SUBCONTRACTOR_ID;
    public static final UUID DRIVER_ID = UUID.fromString("0192f0a0-0000-7000-8000-000000000004");
    public static final UUID SHIPMENT_ID = UUID.fromString("0192f0a0-0000-7000-8000-0000000000aa");
    public static final Instant CREATED_AT = Instant.parse("2026-10-21T06:00:00Z");
    public static final int BAG_COUNT = 40;
    public static final String WAYBILL_NO = "IRS-2026-0001";

    private TestShipments() {}

    /** Çıkış tartımı girilmiş sevkiyat; tamamlanmışsa irsaliye numarası da vardır. */
    public static Shipment shipment(ShipmentStatus status) {
        Weighing dispatch = new Weighing(
                new BigDecimal("2000.5"), new BigDecimal("18"), BAG_COUNT, CREATED_AT.plus(Duration.ofHours(1)));
        return new Shipment(
                SHIPMENT_ID,
                "SV-2026-000123",
                SITE_ID,
                status,
                new ShipmentParties(SUBCONTRACTOR_ID, "Örnek Tekstil", DRIVER_ID, "Ali Yılmaz", "34ABC123"),
                new PlannedLoad(BAG_COUNT, new BigDecimal("1982.5")),
                dispatch,
                status == ShipmentStatus.COMPLETED ? WAYBILL_NO : null,
                CREATED_AT,
                Shipment.RecipeState.LEGACY);
    }

    /** Çıkış tartımı henüz girilmemiş sevkiyat. */
    public static Shipment withoutDispatch(ShipmentStatus status) {
        Shipment shipment = shipment(status);
        return new Shipment(
                shipment.id(),
                shipment.shipmentNo(),
                shipment.siteId(),
                status,
                shipment.parties(),
                shipment.planned(),
                null,
                null,
                shipment.createdAt(),
                Shipment.RecipeState.LEGACY);
    }
}
