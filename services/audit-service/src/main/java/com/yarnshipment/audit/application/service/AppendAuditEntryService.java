package com.yarnshipment.audit.application.service;

import com.yarnshipment.audit.application.port.in.AppendAuditEntryCommand;
import com.yarnshipment.audit.application.port.in.AppendAuditEntryUseCase;
import com.yarnshipment.audit.application.port.out.AuditAppendPort;
import org.springframework.stereotype.Service;

/** Olayı zincire ekler. Tekrar teslim edilen olay yeni satır üretmez; tekilliği veritabanı fonksiyonu sağlar. */
@Service
public final class AppendAuditEntryService implements AppendAuditEntryUseCase {

    private final AuditAppendPort appendPort;

    public AppendAuditEntryService(AuditAppendPort appendPort) {
        this.appendPort = appendPort;
    }

    @Override
    public long appendEntry(AppendAuditEntryCommand command) {
        return appendPort.append(command);
    }
}
