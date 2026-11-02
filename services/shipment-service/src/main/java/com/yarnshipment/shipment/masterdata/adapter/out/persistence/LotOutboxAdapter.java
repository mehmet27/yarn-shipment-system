package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import com.yarnshipment.events.masterdata.LotQualityLocked;
import com.yarnshipment.shipment.masterdata.application.port.out.LotEventPort;
import com.yarnshipment.shipment.masterdata.domain.lot.ProductionLot;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Parti olaylarını servis outbox'ına yazar (EVT-03); yayıncı onları sevkiyat olaylarıyla aynı konuya gönderir. Olay
 * kimliği UUIDv7'dir. Paket ayrı master-data servisine taşınacağı için sevkiyat adapter'larına bağlanmaz.
 */
@Component
final class LotOutboxAdapter implements LotEventPort {

    private static final String AGGREGATE_TYPE = "ProductionLot";
    private static final String INSERT_OUTBOX = """
            INSERT INTO outbox_event (id, created_at, aggregate_type, aggregate_id, event_type, payload, headers,
                                      occurred_at)
            VALUES (:id, :at, :type, :aggregateId, :eventType, CAST(:payload AS jsonb), CAST(:headers AS jsonb),
                    :occurredAt)""";
    private static final int TIMESTAMP_SHIFT = 16;
    private static final long VERSION_7 = 0x7000L;
    private static final long RANDOM_12_BITS = 0x0FFFL;
    private static final long VARIANT_MASK = 0x3FFF_FFFF_FFFF_FFFFL;
    private static final long VARIANT_RFC = 0x8000_0000_0000_0000L;

    private final SecureRandom random = new SecureRandom();
    private final JdbcClient jdbc;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    LotOutboxAdapter(JdbcClient jdbc, JsonMapper jsonMapper, Clock clock) {
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    @Override
    public void publishQualityLocked(UUID actorId, ProductionLot lot) {
        Instant lockedAt = Objects.requireNonNull(lot.qualityLockedAt(), "qualityLockedAt");
        LotQualityLocked event = new LotQualityLocked(
                lot.id(),
                lot.siteId(),
                lot.details().lotNo(),
                lot.details().productId(),
                lot.labels().qualityGradeCode(),
                lockedAt);
        Instant now = clock.instant();
        jdbc.sql(INSERT_OUTBOX)
                .param("id", nextEventId(now))
                .param("at", Timestamp.from(now))
                .param("type", AGGREGATE_TYPE)
                .param("aggregateId", lot.id())
                .param("eventType", LotQualityLocked.TYPE)
                .param("payload", jsonMapper.writeValueAsString(event))
                .param("headers", jsonMapper.writeValueAsString(Map.of("actorId", actorId.toString())))
                .param("occurredAt", Timestamp.from(lockedAt))
                .update();
    }

    /** RFC 9562 UUIDv7: ilk 48 bit milisaniye, kalanı rastgele. */
    private UUID nextEventId(Instant now) {
        long mostSignificant =
                (now.toEpochMilli() << TIMESTAMP_SHIFT) | VERSION_7 | (random.nextLong() & RANDOM_12_BITS);
        long leastSignificant = (random.nextLong() & VARIANT_MASK) | VARIANT_RFC;
        return new UUID(mostSignificant, leastSignificant);
    }
}
