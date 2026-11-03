package com.yarnshipment.audit.application.port.in;

import java.util.OptionalLong;

/** Son doğrulamadan sonra eklenen kayıtların zincirini doğrular. */
public interface VerifyAuditChainUseCase {

    /** İlk bozuk kaydın sırasını döner; bozulma yoksa ya da doğrulanacak yeni kayıt yoksa boş. */
    OptionalLong verifyChain();
}
