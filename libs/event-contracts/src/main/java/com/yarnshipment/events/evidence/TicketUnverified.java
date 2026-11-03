package com.yarnshipment.events.evidence;

import java.time.Instant;
import java.util.UUID;

/**
 * FisDogrulanamadi: kantar fişi üç denemede okunamadı ve kullanıcı gerekçe seçip devam etti (tasarım K0). Fotoğraf
 * uyuşmazlığını tetikler; sevkiyatı durdurmaz.
 *
 * @param evidenceId son denemenin fotoğrafı
 * @param shipmentId sevkiyat
 * @param evidenceType {@code DISPATCH_SCALE} veya {@code DELIVERY_SCALE}
 * @param reason gerekçe, ör. {@code TICKET_DAMAGED}
 * @param declaredAt gerekçenin seçildiği zaman
 */
public record TicketUnverified(
        UUID evidenceId, UUID shipmentId, String evidenceType, String reason, Instant declaredAt) {

    public static final String TYPE = "com.yarnshipment.evidence.ticket-unverified.v1";
}
