package com.yarnshipment.audit.adapter.in.web;

import com.yarnshipment.audit.adapter.in.web.api.model.AuditActor;
import com.yarnshipment.audit.adapter.in.web.api.model.AuditEntity;
import com.yarnshipment.audit.adapter.in.web.api.model.AuditRecordPage;
import com.yarnshipment.audit.adapter.in.web.api.model.AuditRecordView;
import com.yarnshipment.audit.adapter.in.web.api.model.ChainCheckView;
import com.yarnshipment.audit.adapter.in.web.api.model.PageMetadata;
import com.yarnshipment.audit.domain.model.AuditRecord;
import com.yarnshipment.audit.domain.model.ChainCheck;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Denetim modelinden API sözleşmesine dönüşüm. Zamanlar UTC'dir (API-03). */
final class AuditViews {

    private AuditViews() {}

    static @Nullable Instant instantOf(@Nullable OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }

    static AuditRecordPage toPage(com.yarnshipment.audit.application.port.in.AuditRecordPage page) {
        PageMetadata metadata = new PageMetadata(page.number(), page.size(), page.totalElements(), page.totalPages());
        List<AuditRecordView> items =
                page.items().stream().map(AuditViews::toView).toList();
        return new AuditRecordPage(items, metadata);
    }

    static AuditRecordView toView(AuditRecord record) {
        AuditActor actor = new AuditActor()
                .userId(record.actor().userId())
                .role(record.actor().role())
                .clientId(record.actor().clientId());
        AuditRecordView view = new AuditRecordView(
                record.id(),
                utc(record.occurredAt()),
                utc(record.recordedAt()),
                record.origin().sourceService(),
                record.origin().eventId(),
                actor,
                record.action());
        view.setDecision(record.decision() == null ? null : AuditRecordView.DecisionEnum.fromValue(record.decision()));
        view.setEntity(record.entity() == null ? null : toEntity(record.entity()));
        view.setBefore(record.before());
        view.setAfter(record.after());
        view.setTraceId(record.origin().traceId());
        return view;
    }

    static ChainCheckView toView(ChainCheck check) {
        return new ChainCheckView(check.id(), utc(check.checkedAt()), check.fromId(), check.toId(), check.ok())
                .brokenAtId(check.brokenAtId());
    }

    private static AuditEntity toEntity(AuditRecord.Entity entity) {
        return new AuditEntity(entity.type()).id(entity.id());
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
