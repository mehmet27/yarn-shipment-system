package com.yarnshipment.reporting.application.port.out;

import com.yarnshipment.reporting.domain.model.BalanceMatrixRow;
import com.yarnshipment.reporting.domain.model.FireFile;
import com.yarnshipment.reporting.domain.model.OpeningBalanceRow;
import com.yarnshipment.reporting.domain.model.ReportPeriod;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Bakiye hareketi ve fire dosyası okuma modeli. */
public interface BalanceFirePort {

    List<BalanceMatrixRow> findBalanceMatrix(ReportScope scope);

    /** Tamamlanma tarihi dönemde olan dosyalar; en yeni önce. */
    List<FireFile> findFireFiles(ReportPeriod period, ReportScope scope, @Nullable UUID subcontractorId);

    /** Satır yazıldıysa true; (fasoncu, iplik) için OPENING varsa false. */
    boolean insertOpeningIfAbsent(OpeningBalanceRow row, Instant occurredAt);
}
