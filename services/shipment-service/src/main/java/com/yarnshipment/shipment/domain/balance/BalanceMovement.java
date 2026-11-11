package com.yarnshipment.shipment.domain.balance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Bakiye defterinin bir satırı; yalnız eklenir, değiştirilmez ve silinmez (KR-13).
 *
 * @param id hareket
 * @param subcontractorId fasoncu
 * @param yarn iplik cinsi ve varsa lot
 * @param shipmentId ilgili sevkiyat, varsa
 * @param kind hareket türü
 * @param netKg işaretli net kg: fasoncuya giden artı, düşen eksi
 * @param occurredAt hareketin zamanı
 * @param createdBy giren kişi
 * @param reason gerekçe veya not; düzeltmede zorunlu
 * @param sourceRef aynı kaynağın tekrar yazılmasını önleyen anahtar
 */
public record BalanceMovement(
        UUID id,
        UUID subcontractorId,
        YarnRef yarn,
        @Nullable UUID shipmentId,
        MovementKind kind,
        BigDecimal netKg,
        Instant occurredAt,
        UUID createdBy,
        @Nullable String reason,
        @Nullable String sourceRef) {

    public BalanceMovement {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(subcontractorId, "subcontractorId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(createdBy, "createdBy");
        Objects.requireNonNull(yarn, "yarn");
        netKg = Kilograms.round(Objects.requireNonNull(netKg, "netKg"));
        if (!kind.accepts(netKg)) {
            throw new InvalidLedgerInputException("netKg");
        }
        if (kind == MovementKind.CORRECTION && (reason == null || reason.isBlank())) {
            throw new InvalidLedgerInputException("reason");
        }
    }

    /** Tamamlanan sevkiyatın bir reçete satırı: fasoncunun bakiyesine artı yazılır. */
    public static BalanceMovement dispatch(DispatchLine line, DispatchContext context) {
        return new BalanceMovement(
                UUID.randomUUID(),
                context.subcontractorId(),
                new YarnRef(line.yarnType(), line.lotId()),
                context.shipmentId(),
                MovementKind.DISPATCH,
                line.netKg(),
                context.at(),
                context.createdBy(),
                null,
                "DISPATCH:" + context.shipmentId() + ":" + line.lineNo());
    }

    /** Bakiyeden düşen hareket (kumaş girişi, dönen, gelen ya da aktarılan iplik); kilo pozitif verilir. */
    public static BalanceMovement consumption(Consumption consumption) {
        return new BalanceMovement(
                UUID.randomUUID(),
                consumption.subcontractorId(),
                new YarnRef(consumption.yarnType(), consumption.lotId()),
                consumption.shipmentId(),
                consumption.kind(),
                consumption.netKg().negate(),
                consumption.at(),
                consumption.createdBy(),
                null,
                consumption.sourceRef());
    }

    /** Haklı kalite itirazının iplik cinsine düşen kilosu fasoncunun bakiyesinden düşer (KR-15); kilo pozitif. */
    public static BalanceMovement claimAdjustment(ClaimShare share, ClaimContext context) {
        return new BalanceMovement(
                UUID.randomUUID(),
                context.subcontractorId(),
                new YarnRef(share.yarnType(), null),
                context.shipmentId(),
                MovementKind.CLAIM_ADJUSTMENT,
                share.netKg().negate(),
                context.at(),
                context.createdBy(),
                "Haklı kalite itirazı",
                "CLAIM:" + context.discrepancyId() + ":" + share.yarnType());
    }

    /** Fasoncudan fasoncuya sevkiyatta gönderen fasoncunun bakiyesinden aynı kilo düşer. */
    public static BalanceMovement transferOut(UUID source, DispatchLine line, DispatchContext context) {
        return consumption(new Consumption(
                MovementKind.TRANSFER_OUT,
                source,
                context.shipmentId(),
                line.yarnType(),
                line.lotId(),
                line.netKg(),
                context.at(),
                context.createdBy(),
                "TRANSFER:" + context.shipmentId() + ":" + line.lineNo()));
    }

    /** Haklı itirazın bağlamı: hangi uyuşmazlık, fasoncu, sevkiyat, ne zaman, kim. */
    public record ClaimContext(UUID discrepancyId, UUID subcontractorId, UUID shipmentId, Instant at, UUID createdBy) {}

    /** Haklı itiraz kilosunun bir iplik cinsine düşen payı, pozitif. */
    public record ClaimShare(String yarnType, BigDecimal netKg) {}

    /** Sevkiyatın tamamlanma bağlamı: hangi fasoncu, hangi sevkiyat, ne zaman, kim. */
    public record DispatchContext(UUID subcontractorId, UUID shipmentId, Instant at, UUID createdBy) {}

    /** Sevkiyatın bir reçete satırı. */
    public record DispatchLine(
            int lineNo, String yarnType, @Nullable UUID lotId, BigDecimal netKg) {}

    /**
     * Bakiyeden düşen hareketin girdisi.
     *
     * @param kind FABRIC_RECEIPT, YARN_RETURN, YARN_FROM_SUBCONTRACTOR veya TRANSFER_OUT
     * @param netKg düşülecek kilo, pozitif
     */
    public record Consumption(
            MovementKind kind,
            UUID subcontractorId,
            UUID shipmentId,
            String yarnType,
            @Nullable UUID lotId,
            BigDecimal netKg,
            Instant at,
            UUID createdBy,
            String sourceRef) {}
}
