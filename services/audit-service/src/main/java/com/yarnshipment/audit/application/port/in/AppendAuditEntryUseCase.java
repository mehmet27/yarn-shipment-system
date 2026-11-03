package com.yarnshipment.audit.application.port.in;

/** Bir olayı denetim zincirine ekler. */
public interface AppendAuditEntryUseCase {

    /** Eklenen kaydın sırasını döner; aynı olay daha önce yazılmışsa mevcut kaydın sırası. */
    long appendEntry(AppendAuditEntryCommand command);
}
