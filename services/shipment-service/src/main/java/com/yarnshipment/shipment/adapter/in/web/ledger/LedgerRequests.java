package com.yarnshipment.shipment.adapter.in.web.ledger;

import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import com.yarnshipment.shipment.domain.balance.Kilograms;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Bakiye, kumaş, iplik dönüşü ve fire isteklerinin gövdeleri; kilolar ondalık dize olarak gelir (API-03). */
final class LedgerRequests {

    private LedgerRequests() {}

    /** Kumaş girişi gövdesi. */
    record FabricReceiptRequest(
            @Nullable String fabricType,
            @Nullable String netKg,
            @Nullable Integer rollCount,
            @Nullable String waybillNo,
            @Nullable Instant receivedAt,
            @Nullable List<YarnShareRequest> yarns) {}

    /** Kumaşın tükettiği iplik. */
    record YarnShareRequest(
            @Nullable String yarnType, @Nullable String netKg) {}

    /** İplik dönüşü gövdesi. */
    record YarnReturnRequest(
            @Nullable String direction,
            @Nullable String yarnType,
            @Nullable UUID lotId,
            @Nullable String netKg,
            @Nullable String waybillNo) {}

    /** Merkeze iplik girişi gövdesi. */
    record YarnReceiptRequest(
            @Nullable String yarnType,
            @Nullable String netKg,
            @Nullable String note) {}

    /** Merkez fabrika kumaş girişi gövdesi. */
    record CentralProductionRequest(
            @Nullable String fabricType,
            @Nullable String fireRate,
            @Nullable Integer rollCount,
            @Nullable String waybillNo,
            @Nullable List<YarnShareRequest> yarns) {}

    /** Fire oranı değişikliği gövdesi. */
    record FireRateRequest(
            @Nullable String fireRate, @Nullable String reason) {}

    /** "Fatura edildi" gövdesi. */
    record InvoiceRequest(
            @Nullable String invoiceNo,
            @Nullable LocalDate invoicedOn,
            @Nullable String note) {}

    static String required(@Nullable String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidLedgerInputException(field);
        }
        return value;
    }

    static LocalDate requiredDate(@Nullable LocalDate value, String field) {
        if (value == null) {
            throw new InvalidLedgerInputException(field);
        }
        return value;
    }

    static BigDecimal kg(@Nullable String value, String field) {
        return Kilograms.parsePositive(required(value, field), field);
    }

    static BigDecimal rate(@Nullable String value) {
        try {
            return new BigDecimal(required(value, "fireRate").strip());
        } catch (NumberFormatException e) {
            throw new InvalidLedgerInputException("fireRate", e);
        }
    }
}
