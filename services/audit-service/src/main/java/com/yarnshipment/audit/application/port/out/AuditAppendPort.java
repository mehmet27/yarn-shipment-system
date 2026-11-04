package com.yarnshipment.audit.application.port.out;

import com.yarnshipment.audit.application.port.in.AppendAuditEntryCommand;

/** Zincire tek ekleme yolu; yazan rolün başka yetkisi yoktur. */
public interface AuditAppendPort {

    /** Kaydı ekler; aynı olay varsa mevcut kaydın sırasını döner. */
    long append(AppendAuditEntryCommand command);
}
