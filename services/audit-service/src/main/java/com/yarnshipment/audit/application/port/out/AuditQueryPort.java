package com.yarnshipment.audit.application.port.out;

import com.yarnshipment.audit.application.port.in.AuditRecordPage;
import com.yarnshipment.audit.application.port.in.ListAuditRecordsQuery;
import com.yarnshipment.audit.domain.model.ChainCheck;
import java.util.List;

/** Denetim verisinin salt okunur görünümü (okuma rolüyle ayrı bağlantı). */
public interface AuditQueryPort {

    AuditRecordPage findRecords(ListAuditRecordsQuery query);

    List<ChainCheck> findLatestChainChecks(int limit);

    /** Zincirdeki son kaydın sırası; kayıt yoksa 0. */
    long latestRecordId();

    /** Doğrulanmış son kaydın sırası; hiç doğrulama yapılmadıysa 0. */
    long lastVerifiedId();
}
