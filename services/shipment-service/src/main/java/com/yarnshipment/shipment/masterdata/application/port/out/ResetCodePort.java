package com.yarnshipment.shipment.masterdata.application.port.out;

import com.yarnshipment.shipment.masterdata.domain.model.ResetCode;
import java.util.Optional;
import java.util.UUID;

/** Şifre sıfırlama kodlarının deposu; kullanıcı başına tek kod tutulur. */
public interface ResetCodePort {

    /** Kullanıcının kodunu yenisiyle değiştirir (deneme sayacı sıfırlanır). */
    void save(ResetCode code);

    Optional<ResetCode> find(UUID userId);

    /** Yanlış deneme sayısını bir artırır. */
    void recordFailedAttempt(UUID userId);

    void delete(UUID userId);
}
