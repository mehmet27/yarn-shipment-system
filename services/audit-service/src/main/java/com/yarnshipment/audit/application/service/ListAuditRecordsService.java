package com.yarnshipment.audit.application.service;

import com.yarnshipment.audit.application.port.in.AuditRecordPage;
import com.yarnshipment.audit.application.port.in.ListAuditRecordsQuery;
import com.yarnshipment.audit.application.port.in.ListAuditRecordsUseCase;
import com.yarnshipment.audit.application.port.out.AuditQueryPort;
import org.springframework.stereotype.Service;

/** Denetim kayıtlarını listeler; erişim izni (AUDIT_VIEW) controller'da denetlenir. */
@Service
public final class ListAuditRecordsService implements ListAuditRecordsUseCase {

    private final AuditQueryPort queryPort;

    public ListAuditRecordsService(AuditQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    @Override
    public AuditRecordPage listAuditRecords(ListAuditRecordsQuery query) {
        return queryPort.findRecords(query);
    }
}
