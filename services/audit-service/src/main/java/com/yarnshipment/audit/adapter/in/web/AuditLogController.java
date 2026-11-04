package com.yarnshipment.audit.adapter.in.web;

import com.yarnshipment.audit.adapter.in.web.api.AuditLogApi;
import com.yarnshipment.audit.adapter.in.web.api.model.AuditRecordPage;
import com.yarnshipment.audit.adapter.in.web.api.model.ChainCheckView;
import com.yarnshipment.audit.application.port.in.ListAuditRecordsQuery;
import com.yarnshipment.audit.application.port.in.ListAuditRecordsUseCase;
import com.yarnshipment.audit.application.port.in.ListChainChecksUseCase;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Denetim izi okuma uçları. Method security proxy'si gerektirdiği için final değildir (SEC-02). */
@RestController
public class AuditLogController implements AuditLogApi {

    private final ListAuditRecordsUseCase listAuditRecords;
    private final ListChainChecksUseCase listChainChecks;

    public AuditLogController(ListAuditRecordsUseCase listAuditRecords, ListChainChecksUseCase listChainChecks) {
        this.listAuditRecords = listAuditRecords;
        this.listChainChecks = listChainChecks;
    }

    @Override
    @PreAuthorize("hasAuthority('AUDIT_VIEW')")
    public ResponseEntity<AuditRecordPage> listAuditRecords(
            @Nullable String entityType,
            @Nullable UUID entityId,
            @Nullable UUID actorUserId,
            @Nullable String action,
            @Nullable String sourceService,
            @Nullable OffsetDateTime from,
            @Nullable OffsetDateTime to,
            Integer page,
            Integer size) {
        ListAuditRecordsQuery.Filter filter = new ListAuditRecordsQuery.Filter(
                entityType,
                entityId,
                actorUserId,
                action,
                sourceService,
                AuditViews.instantOf(from),
                AuditViews.instantOf(to));
        return ResponseEntity.ok(
                AuditViews.toPage(listAuditRecords.listAuditRecords(new ListAuditRecordsQuery(filter, page, size))));
    }

    @Override
    @PreAuthorize("hasAuthority('AUDIT_VIEW')")
    public ResponseEntity<List<ChainCheckView>> listChainChecks(Integer limit) {
        return ResponseEntity.ok(listChainChecks.listChainChecks(limit).stream()
                .map(AuditViews::toView)
                .toList());
    }
}
