package com.yarnshipment.shipment.adapter.in.web.ledger;

import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import com.yarnshipment.shipment.domain.balance.Kilograms;
import com.yarnshipment.shipment.domain.fabric.FabricRecipe;
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
            @Nullable List<YarnRatioRequest> yarns) {}

    /** Kumaş girişinin düşüm önizlemesi gövdesi. */
    record FabricPreviewRequest(
            @Nullable String netKg, @Nullable List<YarnRatioRequest> yarns) {}

    /** Kumaşın bir ipliğe yazılan oranı; oran yüzdedir ("33.5"), en çok iki ondalık. */
    record YarnRatioRequest(
            @Nullable String yarnType,
            @Nullable UUID lotId,
            @Nullable String ratioPct) {}

    /** Kumaş reçetesi gövdesi; {@code active} yalnız değiştirirken okunur. */
    record FabricRecipeRequest(
            @Nullable String fabricType,
            @Nullable List<YarnRatioRequest> yarns,
            @Nullable Boolean active) {}

    /** Fatura satırındaki iplik ve kilosu. */
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
            @Nullable UUID productId,
            @Nullable UUID supplierId,
            @Nullable String netKg,
            @Nullable String lotNo,
            @Nullable UUID qualityGradeId,
            @Nullable String waybillNo,
            @Nullable String note) {}

    /** Merkez kumaşın tükettiği iplik tanımı. */
    record CentralYarnRequest(
            @Nullable UUID productId, @Nullable String netKg) {}

    /** Merkez fabrika kumaş girişi gövdesi. */
    record CentralProductionRequest(
            @Nullable String fabricType,
            @Nullable String fireRate,
            @Nullable Integer rollCount,
            @Nullable String waybillNo,
            @Nullable List<CentralYarnRequest> yarns) {}

    /** Fire oranı değişikliği gövdesi. */
    record FireRateRequest(
            @Nullable String fireRate, @Nullable String reason) {}

    /** "Fatura edildi" gövdesi. */
    record InvoiceRequest(
            @Nullable String invoiceNo,
            @Nullable LocalDate invoicedOn,
            @Nullable String note,
            @Nullable List<YarnShareRequest> lines) {}

    /** Karışım satırları; oran toplamı ve tekrar denetimi {@code FabricRecipe} kurulurken yapılır. */
    static List<FabricRecipe.Line> recipeLines(@Nullable List<YarnRatioRequest> requests) {
        if (requests == null) {
            throw new InvalidLedgerInputException("yarns");
        }
        return requests.stream()
                .map(request -> new FabricRecipe.Line(
                        required(request.yarnType(), "yarns.yarnType"), ratioPct(request.ratioPct(), "yarns.ratioPct")))
                .toList();
    }

    static String required(@Nullable String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidLedgerInputException(field);
        }
        return value;
    }

    static UUID requiredId(@Nullable UUID value, String field) {
        if (value == null) {
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

    static BigDecimal ratioPct(@Nullable String value, String field) {
        try {
            return new BigDecimal(required(value, field).strip());
        } catch (NumberFormatException e) {
            throw new InvalidLedgerInputException(field, e);
        }
    }

    static BigDecimal rate(@Nullable String value) {
        try {
            return new BigDecimal(required(value, "fireRate").strip());
        } catch (NumberFormatException e) {
            throw new InvalidLedgerInputException("fireRate", e);
        }
    }
}
