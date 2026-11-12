package com.yarnshipment.audit.adapter.in.messaging;

import com.yarnshipment.audit.application.port.in.AppendAuditEntryUseCase;
import com.yarnshipment.common.outbox.DomainEventConsumer;
import com.yarnshipment.common.outbox.IncomingEvent;
import org.springframework.stereotype.Component;

/**
 * Tüm modüllerin olaylarını zincire yazar (SEC-06); taşıyıcıdan bağımsızdır. Aynı olayın tekrar teslimi veritabanında
 * tek kayıt bırakır; bu yüzden işlenmiş olay tablosu tutulmaz.
 *
 * <p>Teslim: {@link Delivery#BEFORE_COMMIT}. Denetim kaydı iş işlemiyle aynı transaction'da yazılır: iş geri alınırsa
 * kayıt da yazılmaz, kayıt yazılamazsa iş de tamamlanmaz. {@code audit_chain_head} kilidi commit'e yakın alınır.
 */
@Component
final class AuditEventsConsumer implements DomainEventConsumer {

    private final AppendAuditEntryUseCase appendAuditEntryUseCase;

    AuditEventsConsumer(AppendAuditEntryUseCase appendAuditEntryUseCase) {
        this.appendAuditEntryUseCase = appendAuditEntryUseCase;
    }

    @Override
    public Delivery delivery() {
        return Delivery.BEFORE_COMMIT;
    }

    @Override
    public void consume(IncomingEvent event) {
        appendAuditEntryUseCase.appendEntry(AuditEntryMapper.toCommand(event));
    }
}
