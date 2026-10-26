package com.yarnshipment.shipment.application.port.out;

import com.yarnshipment.shipment.domain.check.CheckConclusion;

/** Kontrol sonucunun yazımı: durum, geçmiş ve tamamlandıysa olay aynı transaction'da. */
public interface CheckConclusionPort {

    void recordConclusion(CheckConclusion conclusion);
}
