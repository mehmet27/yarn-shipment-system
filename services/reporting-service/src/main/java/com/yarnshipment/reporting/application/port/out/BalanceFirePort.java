package com.yarnshipment.reporting.application.port.out;

import com.yarnshipment.reporting.domain.model.BalanceMatrixRow;
import com.yarnshipment.reporting.domain.model.FireFile;
import com.yarnshipment.reporting.domain.model.OpeningBalanceRow;
import com.yarnshipment.reporting.domain.model.ReportPeriod;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Bakiye hareketi ve fire dosyası okuma modeli. */
public interface BalanceFirePort {

    /** Yola çıkan iplik tutarı ve fiyatsız kilosu. */
    record ShippedAmount(BigDecimal amount, BigDecimal unpricedKg) {}

    /** Fasoncudaki bir iplik cinsinin kilosu ve son fiyatı (yoksa null). */
    record PricedBalance(
            UUID subcontractorId,
            String subcontractorName,
            BigDecimal balanceKg,
            @Nullable BigDecimal unitPrice) {}

    List<BalanceMatrixRow> findBalanceMatrix(ReportScope scope);

    /** Tamamlanma tarihi dönemde olan dosyalar; en yeni önce. */
    List<FireFile> findFireFiles(ReportPeriod period, ReportScope scope, @Nullable UUID subcontractorId);

    /** Dönemde yola çıkan iplik: (fiyatı girilenlerin tutarı, fiyatsız kilo). */
    ShippedAmount findShippedAmount(ReportPeriod period, ReportScope scope);

    /** Sevkiyat başına kilo ağırlıklı ortalama birim fiyat; hiç fiyatı olmayan sevkiyat sonuçta yer almaz. */
    Map<UUID, BigDecimal> findAveragePrices(Collection<UUID> shipmentIds);

    /** Fasoncudaki iplik kiloları ve son girilen fiyatları (fasoncu × iplik cinsi). */
    List<PricedBalance> findPricedBalances(ReportScope scope);

    /** Satır defterine yazıldıysa true; (fasoncu, iplik) için OPENING varsa ya da kilo sıfırsa false. */
    boolean insertOpeningIfAbsent(OpeningBalanceRow row, Instant occurredAt, UUID actorId);
}
