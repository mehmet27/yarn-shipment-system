package com.yarnshipment.audit.application.service;

import com.yarnshipment.audit.application.port.in.ListChainChecksUseCase;
import com.yarnshipment.audit.application.port.out.AuditQueryPort;
import com.yarnshipment.audit.domain.model.ChainCheck;
import java.util.List;
import org.springframework.stereotype.Service;

/** Zincir doğrulama sonuçlarını listeler. */
@Service
public final class ListChainChecksService implements ListChainChecksUseCase {

    private final AuditQueryPort queryPort;

    public ListChainChecksService(AuditQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    @Override
    public List<ChainCheck> listChainChecks(int limit) {
        return queryPort.findLatestChainChecks(limit);
    }
}
