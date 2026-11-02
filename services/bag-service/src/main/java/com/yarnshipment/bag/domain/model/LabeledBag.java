package com.yarnshipment.bag.domain.model;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * Çuval ve etiketinin baskı geçmişinin özeti.
 *
 * @param printCount etiketin kaç kez basıldığı; 0 ise hiç basılmadı
 * @param lastPrintedAt son baskı; hiç basılmadıysa {@code null}
 */
public record LabeledBag(Bag bag, int printCount, @Nullable Instant lastPrintedAt) {}
