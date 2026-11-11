package com.yarnshipment.reporting.application.service;

import com.yarnshipment.reporting.application.port.in.ImportOpeningBalanceUseCase;
import com.yarnshipment.reporting.application.port.in.ListBalanceMatrixUseCase;
import com.yarnshipment.reporting.application.port.in.ListFireReportUseCase;
import com.yarnshipment.reporting.application.port.in.ListToInvoiceUseCase;
import com.yarnshipment.reporting.application.port.in.ListYarnVsFabricUseCase;
import com.yarnshipment.reporting.application.port.in.ReportQuery;
import com.yarnshipment.reporting.application.port.out.BalanceFirePort;
import com.yarnshipment.reporting.domain.model.BalanceMatrixRow;
import com.yarnshipment.reporting.domain.model.FireFile;
import com.yarnshipment.reporting.domain.model.OpeningBalanceRow;
import com.yarnshipment.reporting.domain.model.OpeningImportResult;
import com.yarnshipment.reporting.domain.model.ReportPeriod;
import com.yarnshipment.reporting.domain.model.YarnVsFabricRow;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bakiye matrisi, fire raporları ve açılış bakiyesi içe aktarma. Kapsam dışı kullanıcı boş rapor alır. */
@Service
public class BalanceFireService
        implements ListBalanceMatrixUseCase,
                ListFireReportUseCase,
                ListYarnVsFabricUseCase,
                ListToInvoiceUseCase,
                ImportOpeningBalanceUseCase {

    private final BalanceFirePort port;
    private final ReportAccess access;
    private final Clock clock;

    BalanceFireService(BalanceFirePort port, ReportAccess access, Clock clock) {
        this.port = port;
        this.access = access;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BalanceMatrixRow> listBalanceMatrix(ReportQuery query) {
        return access.scopeOf(query).map(port::findBalanceMatrix).orElseGet(List::of);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FireFile> listFire(ReportQuery query, @Nullable UUID subcontractorId) {
        ReportPeriod period = periodOf(query);
        return access.scopeOf(query)
                .map(scope -> port.findFireFiles(period, scope, subcontractorId))
                .orElseGet(List::of);
    }

    @Override
    @Transactional(readOnly = true)
    public List<YarnVsFabricRow> listYarnVsFabric(ReportQuery query) {
        return YarnVsFabricRow.aggregate(listFire(query, null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FireFile> listToInvoice(ReportQuery query) {
        return listFire(query, null).stream()
                .filter(f -> "TO_INVOICE".equals(f.status()) && f.shortageKg().signum() > 0)
                .toList();
    }

    @Override
    @Transactional
    public OpeningImportResult importOpening(LocalDate asOf, List<OpeningBalanceRow> rows) {
        var occurredAt = asOf.atStartOfDay(ReportPeriod.ZONE).toInstant();
        int imported = 0;
        for (OpeningBalanceRow row : rows) {
            if (port.insertOpeningIfAbsent(row, occurredAt)) {
                imported++;
            }
        }
        return new OpeningImportResult(imported, rows.size() - imported);
    }

    private ReportPeriod periodOf(ReportQuery query) {
        return ReportPeriod.of(query.from(), query.to(), LocalDate.ofInstant(clock.instant(), ReportPeriod.ZONE));
    }
}
