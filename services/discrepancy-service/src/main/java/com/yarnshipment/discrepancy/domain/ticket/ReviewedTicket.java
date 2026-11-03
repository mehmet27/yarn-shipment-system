package com.yarnshipment.discrepancy.domain.ticket;

import java.math.BigDecimal;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Uyuşmazlık ekranında gösterilen fiş: okunan değerler, aynı taraf için elle girilen net ve varsa "AI yanıldı" işareti.
 *
 * @param reading fiş okuması
 * @param enteredNetKg elle girilen net; teslim karşılaştırması yapılmadıysa boş
 * @param misread "AI yanıldı" işareti; yoksa boş
 */
public record ReviewedTicket(
        TicketReading reading,
        @Nullable BigDecimal enteredNetKg,
        @Nullable TicketMisread misread) {

    public ReviewedTicket {
        Objects.requireNonNull(reading, "reading");
    }
}
