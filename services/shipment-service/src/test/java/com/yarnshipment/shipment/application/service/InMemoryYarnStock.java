package com.yarnshipment.shipment.application.service;

import com.yarnshipment.shipment.application.port.out.ledger.YarnStockPort;
import com.yarnshipment.shipment.domain.fabric.YarnStockRow;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Fasoncunun açık sevkiyatlardaki iplik portunun sahtesi: satırları testin verdiği değerlerdir. */
// İstisna: SIZE-04, sahte satırı testin okunur yazabilmesi için alanları tek tek alır
@SuppressWarnings({"checkstyle:ParameterNumber", "checkstyle:MagicNumber"})
final class InMemoryYarnStock implements YarnStockPort {

    private final List<YarnStockRow> rows = new ArrayList<>();

    /** Sevkiyatta kalan bir iplik satırı ekler; partisiz. */
    void givenYarn(UUID shipmentId, String shipmentNo, String since, String yarnName, String kg) {
        givenLot(shipmentId, shipmentNo, since, yarnName, null, kg);
    }

    void givenLot(UUID shipmentId, String shipmentNo, String since, String yarnName, @Nullable UUID lotId, String kg) {
        rows.add(new YarnStockRow(
                shipmentId,
                shipmentNo,
                Instant.parse(since),
                yarnName,
                yarnName,
                lotId,
                lotId == null ? null : "L-" + lotId.toString().substring(0, 4),
                null,
                new BigDecimal(kg)));
    }

    @Override
    public List<YarnStockRow> findOpenStock(UUID subcontractorId) {
        return List.copyOf(rows);
    }
}
