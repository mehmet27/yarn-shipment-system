package com.yarnshipment.discrepancy.application.service;

import com.yarnshipment.common.security.scope.AccessScope;
import com.yarnshipment.discrepancy.application.port.in.CloseMissingDocumentCommand;
import com.yarnshipment.discrepancy.application.port.in.CloseMissingDocumentUseCase;
import com.yarnshipment.discrepancy.application.port.in.ListDiscrepanciesQuery;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyDecisionPort;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyQueryPort;
import com.yarnshipment.discrepancy.domain.model.ComparisonRule;
import com.yarnshipment.discrepancy.domain.model.Discrepancy;
import com.yarnshipment.discrepancy.domain.model.Resolution;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tutanaktan sonra imzalı teslim belgesi yüklenince sevkiyatın açık {@code DOCUMENT_MISSING} uyuşmazlığını sistem
 * hesabıyla {@code DATA_ERROR} kararıyla kapatır: eksik olan veri sonradan geldi. Diğer belge kuralları (irsaliye
 * alanlarının tutmaması) insan kararı ister ve açık kalır. Olay tekrar gelirse kapatılacak uyuşmazlık kalmadığı için
 * hiçbir şey yazılmaz. Karar olayı ({@code DiscrepancyResolved}) son açık uyuşmazlıkta sevkiyatı tamamlatır.
 */
@Service
public class CloseMissingDocumentService implements CloseMissingDocumentUseCase {

    /** shipment-service'teki sistem hesabıyla aynı; geçmişte "Sistem" olarak görünür. */
    static final UUID SYSTEM_ACTOR = new UUID(0L, 0L);

    private static final int MAX_PER_SHIPMENT = 100;

    private final DiscrepancyQueryPort queryPort;
    private final DiscrepancyDecisionPort decisionPort;
    private final Clock clock;

    public CloseMissingDocumentService(
            DiscrepancyQueryPort queryPort, DiscrepancyDecisionPort decisionPort, Clock clock) {
        this.queryPort = queryPort;
        this.decisionPort = decisionPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public int closeMissingDocument(CloseMissingDocumentCommand command) {
        ListDiscrepanciesQuery.Filter ofShipment =
                new ListDiscrepanciesQuery.Filter(null, null, null, command.shipmentId());
        List<Discrepancy> missing =
                queryPort.findDiscrepancies(ofShipment, AccessScope.UNRESTRICTED, 0, MAX_PER_SHIPMENT).items().stream()
                        .filter(discrepancy -> discrepancy.status().isUnresolved())
                        .filter(discrepancy -> ComparisonRule.DOCUMENT_MISSING
                                .name()
                                .equals(discrepancy.basis().ruleCode()))
                        .toList();
        Instant at = clock.instant();
        for (Discrepancy discrepancy : missing) {
            Discrepancy resolved = discrepancy.resolve(new Discrepancy.Decision(
                    Resolution.DATA_ERROR,
                    "İmzalı teslim belgesi sonradan yüklendi (kanıt %s).".formatted(command.evidenceId()),
                    null,
                    SYSTEM_ACTOR,
                    at));
            decisionPort.saveResolution(resolved, SYSTEM_ACTOR, at);
        }
        return missing.size();
    }
}
