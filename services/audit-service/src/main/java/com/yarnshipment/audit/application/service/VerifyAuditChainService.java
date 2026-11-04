package com.yarnshipment.audit.application.service;

import com.yarnshipment.audit.application.port.in.VerifyAuditChainUseCase;
import com.yarnshipment.audit.application.port.out.AuditChainPort;
import com.yarnshipment.audit.application.port.out.AuditQueryPort;
import java.util.OptionalLong;
import org.springframework.stereotype.Service;

/** Zinciri son doğrulanan kayıttan sonraki aralık için doğrular; bozulma ERROR olarak loglanır (alarm). */
@Service
public final class VerifyAuditChainService implements VerifyAuditChainUseCase {

    private final AuditQueryPort queryPort;
    private final AuditChainPort chainPort;

    public VerifyAuditChainService(AuditQueryPort queryPort, AuditChainPort chainPort) {
        this.queryPort = queryPort;
        this.chainPort = chainPort;
    }

    @Override
    public OptionalLong verifyChain() {
        long from = queryPort.lastVerifiedId() + 1;
        long to = queryPort.latestRecordId();
        if (to < from) {
            return OptionalLong.empty();
        }
        return chainPort.verify(from, to);
    }
}
