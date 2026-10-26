package com.yarnshipment.events.shipment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * TeslimGirisiYapildi: fasoncu kör teslim girişini gönderdi. Çıkış verisini taşır; yalnızca discrepancy-service ve
 * audit-service tüketir (SEC-06).
 *
 * @param shipmentId sevkiyat
 * @param deliveryReceiptId tutanak
 * @param submittedAt gönderim zamanı
 * @param delivery teslim tartısı
 * @param dispatch çıkış tartısı; girilmediyse boş
 * @param expectedUniqueKeys sevkiyattaki çuvalların anahtarları (sıralı)
 * @param scannedUniqueKeys teslimde okutulan anahtarlar (sıralı)
 * @param dispatchSeal çıkış mührü
 * @param deliverySeal teslimdeki mühür
 * @param context lokasyon, sevkiyat numarası ve irsaliye (sonradan eklendi; eski olaylarda boş)
 */
public record DeliveryReceived(
        UUID shipmentId,
        UUID deliveryReceiptId,
        Instant submittedAt,
        Weighing delivery,
        @Nullable Weighing dispatch,
        List<String> expectedUniqueKeys,
        List<String> scannedUniqueKeys,
        Seal dispatchSeal,
        Seal deliverySeal,
        @Nullable Context context) {

    public static final String TYPE = "com.yarnshipment.shipment.delivery-received.v1";

    public DeliveryReceived {
        expectedUniqueKeys = List.copyOf(expectedUniqueKeys);
        scannedUniqueKeys = List.copyOf(scannedUniqueKeys);
    }

    /**
     * Tartı.
     *
     * @param grossKg brüt
     * @param tareKg dara
     * @param netKg net
     * @param bagCount çuval sayısı
     */
    public record Weighing(BigDecimal grossKg, BigDecimal tareKg, BigDecimal netKg, int bagCount) {}

    /**
     * Mühür.
     *
     * @param sealNo numara; mühür yoksa boş
     * @param condition {@code INTACT}, {@code DAMAGED} veya {@code MISSING}; çıkışta boş
     */
    public record Seal(@Nullable String sealNo, @Nullable String condition) {}

    /**
     * Karşılaştırmanın sevkiyata ve irsaliyeye dair bağlamı.
     *
     * @param siteId çıkış lokasyonu
     * @param shipmentNo sevkiyat numarası
     * @param document irsaliye değerleri
     */
    public record Context(UUID siteId, String shipmentNo, Document document) {}

    /**
     * İrsaliye üzerindeki değerler, fasoncunun okuduğu haliyle.
     *
     * @param shipmentNo irsaliyedeki sevkiyat numarası
     * @param date irsaliye tarihi
     * @param netKg irsaliyedeki net kilo
     * @param bagCount irsaliyedeki çuval sayısı
     * @param signedDocumentEvidenceId imzalı irsaliye fotoğrafı
     */
    public record Document(
            @Nullable String shipmentNo,
            @Nullable LocalDate date,
            @Nullable BigDecimal netKg,
            @Nullable Integer bagCount,
            @Nullable UUID signedDocumentEvidenceId) {}
}
