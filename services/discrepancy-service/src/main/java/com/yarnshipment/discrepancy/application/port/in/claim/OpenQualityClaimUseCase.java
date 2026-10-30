package com.yarnshipment.discrepancy.application.port.in.claim;

/**
 * Teslimden sonraki 3 iş günü içinde, teslimde okutulmuş çuvallar için fotoğraflı kalite itirazı açar. İtiraz
 * Kırmızı bir {@code QUALITY} uyuşmazlığı olarak incelemeye girer.
 */
public interface OpenQualityClaimUseCase {

    ClaimFile openQualityClaim(OpenQualityClaimCommand command);
}
