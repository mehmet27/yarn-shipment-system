package com.yarnshipment.shipment.domain.model;

/** Müdürün kantar uyuşmazlığı kararı (süreç analizi 5.4). */
public enum WeightDecision {
    /** Sevkiyat iptal edilir, çuvallar serbest kalır. */
    CANCEL,
    /** Sevkiyat taslağa döner; depo çuval ekler ya da çıkarır ve yeniden tartar. */
    ADD_REMOVE_BAGS,
    /** Reçete kilosu tartıya eşitlenir; sevkiyat yola çıkar. */
    UPDATE_RECIPE
}
