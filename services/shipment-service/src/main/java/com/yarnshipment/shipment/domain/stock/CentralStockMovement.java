package com.yarnshipment.shipment.domain.stock;

import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import com.yarnshipment.shipment.domain.balance.Kilograms;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Merkez iplik stoku defterinin bir satırı; yalnız eklenir.
 *
 * @param id kayıt
 * @param yarnType iplik cinsi
 * @param kind tür
 * @param netKg işaretli net kg: girişte artı, çıkışta eksi
 * @param productionId üretim satırında üretim kaydı
 * @param fabricKg üretim satırında bu iplikten çıkan kumaş kilosu
 * @param note not, isteğe bağlı
 * @param occurredAt hareketin zamanı
 * @param createdBy kaydeden kullanıcı
 */
public record CentralStockMovement(
        UUID id,
        String yarnType,
        CentralStockKind kind,
        BigDecimal netKg,
        @Nullable UUID productionId,
        @Nullable BigDecimal fabricKg,
        @Nullable String note,
        Instant occurredAt,
        UUID createdBy) {

    public static final int MAX_NOTE_LENGTH = 500;

    public CentralStockMovement {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(createdBy, "createdBy");
        yarnType = yarnType.strip();
        if (yarnType.isEmpty()) {
            throw new InvalidLedgerInputException("yarnType");
        }
        netKg = Kilograms.round(netKg);
        boolean incoming = kind == CentralStockKind.YARN_RECEIPT;
        if (netKg.signum() == 0 || incoming != (netKg.signum() > 0)) {
            throw new InvalidLedgerInputException("netKg");
        }
        if (note != null && note.length() > MAX_NOTE_LENGTH) {
            throw new InvalidLedgerInputException("note");
        }
    }

    /** Merkeze iplik girişi. */
    public static CentralStockMovement receipt(
            String yarnType, BigDecimal netKg, @Nullable String note, Instant at, UUID createdBy) {
        String text = note == null || note.isBlank() ? null : note.strip();
        return new CentralStockMovement(
                UUID.randomUUID(), yarnType, CentralStockKind.YARN_RECEIPT, netKg, null, null, text, at, createdBy);
    }
}
