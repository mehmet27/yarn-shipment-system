package com.yarnshipment.audit.application.port.in;

import com.yarnshipment.audit.domain.model.ChainCheck;
import java.util.List;

/** Zincir doğrulama sonuçlarının en yenilerini listeler. */
public interface ListChainChecksUseCase {

    List<ChainCheck> listChainChecks(int limit);
}
