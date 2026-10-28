package com.yarnshipment.shipment.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Fasoncu kör teslim girişini tamamladı (olay kataloğu: TeslimGirisiYapildi). Çıkış verisi yalnızca
 * discrepancy-service içindir (SEC-06); kişisel veri taşımaz (EVT-08).
 *
 * @param shipmentId sevkiyat
 * @param deliveryReceiptId tutanak
 * @param submittedAt gönderim zamanı
 * @param delivery teslim tartısı
 * @param dispatch çıkış tartısı; girilmediyse {@code null}
 * @param expectedUniqueKeys sevkiyattaki çuvalların anahtarları
 * @param scannedUniqueKeys teslimde okutulan anahtarlar
 * @param dispatchSeal çıkış mührü
 * @param deliverySeal teslimdeki mühür
 * @param context lokasyon, sevkiyat numarası ve irsaliye (v1'e sonradan eklendi, EVT-06)
 */
public record DeliveryReceived(
        UUID shipmentId,
        UUID deliveryReceiptId,
        Instant submittedAt,
        WeighingFigures delivery,
        @Nullable WeighingFigures dispatch,
        List<String> expectedUniqueKeys,
        List<String> scannedUniqueKeys,
        SealState dispatchSeal,
        SealState deliverySeal,
        Context context) {

    public static final String TYPE = "com.yarnshipment.shipment.delivery-received.v1";

    public DeliveryReceived {
        expectedUniqueKeys = List.copyOf(expectedUniqueKeys);
        scannedUniqueKeys = List.copyOf(scannedUniqueKeys);
    }

    /** Tutanaktan olayı kurar; anahtarlar sıralanır ki yük tekrar üretilebilir olsun. */
    public static DeliveryReceived of(DeliveryReceipt receipt, UUID deliveryReceiptId, List<String> expectedKeys) {
        DeliveryMeasurement measurement = receipt.measurement();
        Weighing dispatchWeighing = receipt.dispatch().weighing();
        return new DeliveryReceived(
                receipt.shipmentId(),
                deliveryReceiptId,
                receipt.receivedAt(),
                WeighingFigures.of(receipt.weighing()),
                dispatchWeighing == null ? null : WeighingFigures.of(dispatchWeighing),
                expectedKeys.stream().sorted().toList(),
                measurement.scannedKeys().stream().sorted().toList(),
                new SealState(receipt.dispatch().sealNo(), null),
                new SealState(measurement.sealNo(), measurement.sealCondition()),
                new Context(receipt.siteId(), receipt.shipmentNo(), receipt.plateNo(), measurement.document()));
    }

    /**
     * Olaydaki tartı.
     *
     * @param grossKg brüt
     * @param tareKg dara
     * @param netKg net
     * @param bagCount çuval sayısı
     */
    public record WeighingFigures(BigDecimal grossKg, BigDecimal tareKg, BigDecimal netKg, int bagCount) {

        static WeighingFigures of(Weighing weighing) {
            return new WeighingFigures(weighing.grossKg(), weighing.tareKg(), weighing.netKg(), weighing.bagCount());
        }
    }

    /**
     * Olaydaki mühür.
     *
     * @param sealNo numara
     * @param condition durum; çıkışta takıldığı için çıkış tarafında boştur
     */
    public record SealState(
            @Nullable String sealNo, @Nullable SealCondition condition) {}

    /**
     * Karşılaştırmanın sevkiyata ve irsaliyeye dair bağlamı.
     *
     * @param siteId çıkış lokasyonu
     * @param shipmentNo sevkiyat numarası
     * @param plateNo araç plakası; araç girilmediyse boş
     * @param document irsaliye değerleri ve imzalı belge
     */
    public record Context(
            UUID siteId, String shipmentNo, @Nullable String plateNo, DeliveryDocument document) {}
}
