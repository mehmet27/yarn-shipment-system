package com.yarnshipment.audit.adapter.in.schedule;

import com.yarnshipment.audit.application.port.in.VerifyAuditChainUseCase;
import java.util.OptionalLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Günlük zincir doğrulama işi. Bozulma ERROR olarak loglanır ve alarm üretir (LOG-04). */
@Component
final class ChainVerificationScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(ChainVerificationScheduler.class);

    private final VerifyAuditChainUseCase verifyAuditChainUseCase;

    ChainVerificationScheduler(VerifyAuditChainUseCase verifyAuditChainUseCase) {
        this.verifyAuditChainUseCase = verifyAuditChainUseCase;
    }

    @Scheduled(cron = "${yarnshipment.audit.chain-verification.cron:0 0 3 * * *}", zone = "UTC")
    void verifyChain() {
        OptionalLong broken = verifyAuditChainUseCase.verifyChain();
        if (broken.isPresent()) {
            LOG.error("Audit chain is broken brokenAtId={}", broken.getAsLong());
        }
    }
}
