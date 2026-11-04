package com.yarnshipment.audit.adapter.in.messaging;

import com.yarnshipment.audit.application.port.in.AppendAuditEntryUseCase;
import com.yarnshipment.events.discrepancy.DiscrepancyEvents;
import com.yarnshipment.events.evidence.EvidenceEvents;
import com.yarnshipment.events.shipment.ShipmentEvents;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Tüm servislerin olay konularını dinler ve her olayı zincire yazar (SEC-06: audit-service çıkış verisini taşıyan
 * olayların tüketicisidir). Aynı olayın tekrar teslimi veritabanında tek kayıt bırakır; bu yüzden işlenmiş olay tablosu
 * tutulmaz. Zarfı bozuk olay kalıcı hatadır ve DLT'ye gider.
 */
@Component
final class AuditEventsListener {

    private final AppendAuditEntryUseCase appendAuditEntryUseCase;

    AuditEventsListener(AppendAuditEntryUseCase appendAuditEntryUseCase) {
        this.appendAuditEntryUseCase = appendAuditEntryUseCase;
    }

    @KafkaListener(topics = {ShipmentEvents.TOPIC, EvidenceEvents.TOPIC, DiscrepancyEvents.TOPIC})
    void onEvent(ConsumerRecord<String, String> record) {
        appendAuditEntryUseCase.appendEntry(AuditEntryMapper.toCommand(record));
    }
}
