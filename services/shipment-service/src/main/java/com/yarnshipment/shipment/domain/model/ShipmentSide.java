package com.yarnshipment.shipment.domain.model;

import org.jspecify.annotations.Nullable;

/**
 * Bir tarafın (çıkış veya teslim) girdiği veri. Kör giriş nedeniyle iki taraf ayrı tutulur ve ayrı gösterilir.
 *
 * @param weighing geçerli tartım; henüz girilmediyse {@code null}
 * @param sealNo mühür numarası (çıkışta takılır)
 * @param sealCondition teslimde mührün durumu
 */
public record ShipmentSide(
        @Nullable Weighing weighing,
        @Nullable String sealNo,
        @Nullable SealCondition sealCondition) {

    /** Henüz veri girilmemiş taraf. */
    public static final ShipmentSide EMPTY = new ShipmentSide(null, null, null);
}
