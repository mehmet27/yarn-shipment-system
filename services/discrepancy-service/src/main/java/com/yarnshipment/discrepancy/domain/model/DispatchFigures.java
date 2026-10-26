package com.yarnshipment.discrepancy.domain.model;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/**
 * Çıkışta depo tarafından girilen değerler.
 *
 * @param netKg çıkış net kilo; çıkış tartısı yoksa boş
 * @param bagCount çıkışta tartılan çuval sayısı; çıkış tartısı yoksa boş
 * @param sealNo araca takılan mühür numarası
 */
public record DispatchFigures(
        @Nullable BigDecimal netKg,
        @Nullable Integer bagCount,
        @Nullable String sealNo) {}
