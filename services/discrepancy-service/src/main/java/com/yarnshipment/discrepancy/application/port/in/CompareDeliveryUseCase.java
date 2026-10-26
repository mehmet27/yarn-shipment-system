package com.yarnshipment.discrepancy.application.port.in;

/** Teslim girişini çıkış kayıtlarıyla karşılaştırır, uyuşmazlıkları açar ve sonucu yayınlar. */
public interface CompareDeliveryUseCase {

    void compareDelivery(CompareDeliveryCommand command);
}
