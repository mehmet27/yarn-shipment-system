package com.yarnshipment.audit.application.port.in;

/** Denetim kayıtlarını süzgeçle listeler. */
public interface ListAuditRecordsUseCase {

    AuditRecordPage listAuditRecords(ListAuditRecordsQuery query);
}
