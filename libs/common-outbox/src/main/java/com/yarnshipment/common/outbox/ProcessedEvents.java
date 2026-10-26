package com.yarnshipment.common.outbox;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * İdempotent tüketici (EVT-05): olay kimliği, işlemle aynı transaction'da {@code processed_event} tablosuna yazılır.
 * İkinci kez gelen olay için {@code false} döner; tüketici işlemi yapmadan olayı onaylar.
 */
public final class ProcessedEvents {

    private static final String INSERT = """
            INSERT INTO processed_event (event_id, event_type, processed_at)
            VALUES (:id, :type, :at)
            ON CONFLICT (event_id) DO NOTHING""";

    private final JdbcClient jdbc;
    private final Clock clock;

    public ProcessedEvents(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** Olay ilk kez işleniyorsa kaydeder ve {@code true} döner. Çağıran transaction içinde olmalıdır. */
    public boolean markProcessed(UUID eventId, String eventType) {
        return jdbc.sql(INSERT)
                        .param("id", eventId)
                        .param("type", eventType)
                        .param("at", Timestamp.from(clock.instant()))
                        .update()
                == 1;
    }
}
