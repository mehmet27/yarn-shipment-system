package com.yarnshipment.shipment.domain.fire;

import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import com.yarnshipment.shipment.domain.fabric.FabricExceedsSentYarnException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Sevkiyatın fire dosyası. Kumaş ve iplik toplamları kayıtlardan hesaplanır; burada dosyanın durumu, oranı, gönderilen
 * kg ve fatura işareti tutulur. Kapanmış dosya (INVOICED, CLOSED) değişmez (KR-16, analiz 5.11).
 *
 * @param shipmentId sevkiyat
 * @param subcontractorId fasoncu
 * @param status dosya durumu
 * @param fireRate fire oranı, 0 ile 0,20 arası
 * @param sentKg gönderilen net kg (G)
 * @param invoiceNo fatura numarası, yalnız INVOICED
 * @param invoicedOn fatura tarihi
 * @param invoicedBy işareti koyan müdür
 * @param invoiceNote not
 */
public record FireSettlement(
        UUID shipmentId,
        UUID subcontractorId,
        FireSettlementStatus status,
        BigDecimal fireRate,
        BigDecimal sentKg,
        @Nullable String invoiceNo,
        @Nullable LocalDate invoicedOn,
        @Nullable UUID invoicedBy,
        @Nullable String invoiceNote) {

    public static final BigDecimal DEFAULT_FIRE_RATE = new BigDecimal("0.0400");
    public static final BigDecimal MAX_FIRE_RATE = new BigDecimal("0.2000");
    public static final int RATE_SCALE = 4;
    public static final int MAX_INVOICE_NO_LENGTH = 64;

    public FireSettlement {
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(subcontractorId, "subcontractorId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(fireRate, "fireRate");
        Objects.requireNonNull(sentKg, "sentKg");
    }

    /** Sevkiyat tamamlanınca açılır: varsayılan %4 oranla kumaş bekler. */
    public static FireSettlement open(UUID shipmentId, UUID subcontractorId, BigDecimal sentKg) {
        return open(shipmentId, subcontractorId, sentKg, DEFAULT_FIRE_RATE);
    }

    /** Sevkiyat tamamlanınca açılır: sevkiyatta girilen fire oranıyla kumaş bekler. */
    public static FireSettlement open(UUID shipmentId, UUID subcontractorId, BigDecimal sentKg, BigDecimal fireRate) {
        return new FireSettlement(
                shipmentId,
                subcontractorId,
                FireSettlementStatus.AWAITING_FABRIC,
                fireRate,
                sentKg,
                null,
                null,
                null,
                null);
    }

    /** Gelen kumaşın toplamı gönderilen iplik kilosunu aşamaz; aşarsa {@link FabricExceedsSentYarnException}. */
    public void requireFabricWithinSent(BigDecimal alreadyReceivedKg, BigDecimal newKg) {
        if (alreadyReceivedKg.add(newKg).compareTo(sentKg) > 0) {
            throw new FabricExceedsSentYarnException(shipmentId);
        }
    }

    /** Kapanmış dosyada {@link FireSettlementClosedException}. */
    public void requireOpen() {
        if (status.isFinal()) {
            throw new FireSettlementClosedException(shipmentId);
        }
    }

    /** Yeni oran 0 ile 0,20 arasında olmalıdır (KR-02); kapanmış dosyada değişmez. */
    public FireSettlement withFireRate(BigDecimal newRate) {
        requireOpen();
        if (newRate.signum() < 0 || newRate.compareTo(MAX_FIRE_RATE) > 0 || newRate.scale() > RATE_SCALE) {
            throw new InvalidLedgerInputException("fireRate");
        }
        return new FireSettlement(
                shipmentId,
                subcontractorId,
                status,
                newRate.setScale(RATE_SCALE),
                sentKg,
                invoiceNo,
                invoicedOn,
                invoicedBy,
                invoiceNote);
    }

    /** İlk kumaş girişinden sonra dosya faturalanmaya ya da kapatılmaya hazırdır. */
    public FireSettlement afterFabricReceipt() {
        requireOpen();
        return withStatus(FireSettlementStatus.TO_INVOICE);
    }

    /** Yalnız hatalı kg sıfırdan büyük ve kumaş girmiş dosya faturalanır; işaret geri alınamaz (KR-16). */
    public FireSettlement markInvoiced(FireCalculation calculation, Invoice invoice, UUID by) {
        requireOpen();
        if (status != FireSettlementStatus.TO_INVOICE || !calculation.hasFaultyKg()) {
            throw new InvalidFireSettlementActionException(shipmentId, "NOTHING_TO_INVOICE");
        }
        String number = invoice.invoiceNo().strip();
        if (number.isEmpty() || number.length() > MAX_INVOICE_NO_LENGTH) {
            throw new InvalidLedgerInputException("invoiceNo");
        }
        return new FireSettlement(
                shipmentId,
                subcontractorId,
                FireSettlementStatus.INVOICED,
                fireRate,
                sentKg,
                number,
                invoice.invoicedOn(),
                by,
                invoice.note());
    }

    /** Yalnız hatalı kg sıfır olan dosya kapatılır. */
    public FireSettlement close(FireCalculation calculation) {
        requireOpen();
        if (calculation.hasFaultyKg()) {
            throw new InvalidFireSettlementActionException(shipmentId, "FAULTY_KG_REMAINS");
        }
        return withStatus(FireSettlementStatus.CLOSED);
    }

    private FireSettlement withStatus(FireSettlementStatus newStatus) {
        return new FireSettlement(
                shipmentId,
                subcontractorId,
                newStatus,
                fireRate,
                sentKg,
                invoiceNo,
                invoicedOn,
                invoicedBy,
                invoiceNote);
    }

    /**
     * Fatura işareti girdisi.
     *
     * @param invoiceNo ERP fatura numarası
     * @param invoicedOn fatura tarihi
     * @param note isteğe bağlı not
     */
    public record Invoice(
            String invoiceNo,
            LocalDate invoicedOn,
            @Nullable String note) {}
}
