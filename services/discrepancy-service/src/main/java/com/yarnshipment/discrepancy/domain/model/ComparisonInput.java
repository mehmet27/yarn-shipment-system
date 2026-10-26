package com.yarnshipment.discrepancy.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Bir sevkiyatın iki tarafı: çıkışta depo, teslimde fasoncu (kör giriş) tarafından girilen değerler.
 *
 * @param shipmentId sevkiyat
 * @param siteId çıkış lokasyonu
 * @param shipmentNo sevkiyat numarası; irsaliyeyle karşılaştırılır
 * @param receivedAt teslim girişinin zamanı
 * @param dispatch çıkış tarafı
 * @param delivery teslim tarafı
 * @param expectedKeys sevkiyattaki çuvalların anahtarları
 * @param scannedKeys teslimde okutulan anahtarlar
 * @param document irsaliye değerleri
 */
public record ComparisonInput(
        UUID shipmentId,
        UUID siteId,
        @Nullable String shipmentNo,
        Instant receivedAt,
        DispatchFigures dispatch,
        DeliveryFigures delivery,
        List<String> expectedKeys,
        List<String> scannedKeys,
        DocumentFigures document) {

    public ComparisonInput {
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(siteId, "siteId");
        Objects.requireNonNull(receivedAt, "receivedAt");
        Objects.requireNonNull(dispatch, "dispatch");
        Objects.requireNonNull(delivery, "delivery");
        expectedKeys = List.copyOf(expectedKeys);
        scannedKeys = List.copyOf(scannedKeys);
        Objects.requireNonNull(document, "document");
    }
}
