package com.yarnshipment.evidence.adapter.out.persistence;

import com.yarnshipment.events.evidence.EvidenceInvalidated;
import com.yarnshipment.evidence.application.port.out.EvidenceInvalidationPort;
import com.yarnshipment.evidence.domain.model.EvidenceAlreadyInvalidatedException;
import com.yarnshipment.evidence.domain.model.EvidenceFile;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link EvidenceInvalidationPort}'un JDBC uygulaması. Yalnızca geçerli kayıt güncellenir; veritabanı tetikleyicisi
 * zaten geçersiz kaydın değişmesini ayrıca engeller.
 */
@Component
final class EvidenceInvalidationAdapter implements EvidenceInvalidationPort {

    private static final String SET_ACTOR = "SELECT set_config('app.actor_id', :actor, true)";
    private static final String INVALIDATE = """
            UPDATE evidence_file SET status = 'INVALIDATED', invalidated_reason = :reason, updated_at = :at,
                   updated_by = :actor, version = version + 1
             WHERE id = :id AND status = 'VALID'""";

    private final JdbcClient jdbc;
    private final EvidenceOutbox outbox;

    EvidenceInvalidationAdapter(JdbcClient jdbc, EvidenceOutbox outbox) {
        this.jdbc = jdbc;
        this.outbox = outbox;
    }

    @Override
    public void saveInvalidation(EvidenceFile evidence, UUID invalidatedBy, Instant at) {
        jdbc.sql(SET_ACTOR).param("actor", invalidatedBy.toString()).query().singleValue();
        int updated = jdbc.sql(INVALIDATE)
                .param("id", evidence.id())
                .param("reason", evidence.invalidatedReason())
                .param("at", Timestamp.from(at))
                .param("actor", invalidatedBy)
                .update();
        if (updated == 0) {
            throw new EvidenceAlreadyInvalidatedException(evidence.id());
        }
        outbox.publish(
                new EvidenceOutbox.Origin(evidence.shipmentId(), invalidatedBy, at),
                EvidenceInvalidated.TYPE,
                new EvidenceInvalidated(
                        evidence.id(),
                        evidence.shipmentId(),
                        evidence.declared().type().name(),
                        Objects.requireNonNullElse(evidence.invalidatedReason(), ""),
                        at));
    }
}
