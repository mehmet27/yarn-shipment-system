package com.yarnshipment.discrepancy;

import com.yarnshipment.discrepancy.domain.ticket.ScaleWeights;
import com.yarnshipment.discrepancy.domain.ticket.ShipmentTickets;
import com.yarnshipment.discrepancy.domain.ticket.TicketFields;
import com.yarnshipment.discrepancy.domain.ticket.TicketReading;
import com.yarnshipment.discrepancy.domain.ticket.TicketSide;
import com.yarnshipment.discrepancy.domain.ticket.WeighingEntries;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Elle girişlerle birebir tutan örnek kantar fişleri; testler yalnızca bozduğu alanı değiştirir. */
public final class TestTickets {

    public static final String PLATE = "34ABC123";
    public static final String TICKET_NO = "004512";
    public static final String SCALE = "Merkez Kantar";
    public static final Instant READ_AT = Instant.parse("2026-10-23T08:00:00Z");

    private TestTickets() {}

    /** Net {@link TestComparisons#DISPATCH_KG} olan tartım. */
    public static ScaleWeights weights() {
        return weights("2100.000", "100.000");
    }

    public static ScaleWeights weights(String grossKg, String tareKg) {
        BigDecimal gross = new BigDecimal(grossKg);
        BigDecimal tare = new BigDecimal(tareKg);
        return new ScaleWeights(gross, tare, gross.subtract(tare));
    }

    public static WeighingEntries entries() {
        return new WeighingEntries(weights(), weights(), PLATE);
    }

    public static TicketReading reading(TicketSide side, @Nullable ScaleWeights weights) {
        return reading(UUID.randomUUID(), side, weights, new TicketFields(" 34 abc-123 ", READ_AT, TICKET_NO, SCALE));
    }

    public static TicketReading reading(
            UUID evidenceId, TicketSide side, @Nullable ScaleWeights weights, TicketFields fields) {
        return new TicketReading(
                evidenceId,
                TestComparisons.SHIPMENT,
                side,
                weights,
                fields,
                weights == null ? List.of("WEIGHT_NOT_READ") : List.of(),
                READ_AT);
    }

    public static ShipmentTickets both(ScaleWeights dispatch, ScaleWeights delivery) {
        return new ShipmentTickets(
                reading(TicketSide.DISPATCH_SCALE, dispatch), reading(TicketSide.DELIVERY_SCALE, delivery), List.of());
    }

    public static ShipmentTickets matching() {
        return both(weights(), weights());
    }
}
