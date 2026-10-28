package com.yarnshipment.events.evidence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * FisOkundu: kantar fişi fotoğrafı okundu. Kilolar yalnızca okuma güvenilirse dolu gelir; okunamayan fişte boştur ve
 * sorunlar listelenir. Elle girilen tartımın yerine geçmez, karşılaştırmada ikinci görüştür.
 *
 * @param evidenceId okunan fotoğraf
 * @param shipmentId sevkiyat
 * @param evidenceType {@code DISPATCH_SCALE} veya {@code DELIVERY_SCALE}
 * @param status {@code READ} veya {@code UNREADABLE}
 * @param weights okunan kilolar; okunamadıysa boş
 * @param fields fişteki diğer alanlar; okunamayan alan boş
 * @param problems okuma sorunları, ör. {@code BLURRY}
 * @param readAt okuma zamanı
 */
public record TicketRead(
        UUID evidenceId,
        UUID shipmentId,
        String evidenceType,
        String status,
        @Nullable Weights weights,
        Fields fields,
        List<String> problems,
        Instant readAt) {

    public static final String TYPE = "com.yarnshipment.evidence.ticket-read.v1";

    public TicketRead {
        problems = List.copyOf(problems);
    }

    /**
     * Fişten okunan kilolar; net, brüt − dara olarak hesaplanmıştır.
     *
     * @param grossKg brüt
     * @param tareKg dara
     * @param netKg net
     */
    public record Weights(BigDecimal grossKg, BigDecimal tareKg, BigDecimal netKg) {}

    /**
     * Fişin kilo dışındaki alanları.
     *
     * @param plate araç plakası
     * @param weighedAt tartım zamanı
     * @param ticketNo fiş numarası
     * @param scaleName kantarın ya da firmanın adı
     */
    public record Fields(
            @Nullable String plate,
            @Nullable Instant weighedAt,
            @Nullable String ticketNo,
            @Nullable String scaleName) {}
}
