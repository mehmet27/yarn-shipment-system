package com.yarnshipment.shipment.application.port.in.check;

/** Tüm uyuşmazlıkları çözülen, incelemedeki sevkiyatı tamamlar. */
public interface ConcludeReviewUseCase {

    void concludeReview(ConcludeReviewCommand command);
}
