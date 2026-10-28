package com.yarnshipment.evidence.domain.ticket;

import java.util.Set;

/** Okumada görülen sorunlar. Kilo sorunları yeniden çekim gerektirir; diğerleri bilgi amaçlıdır. */
public enum ReadingProblem {
    /** Brüt veya dara okunamadı ya da model emin değil. */
    WEIGHT_NOT_READ,
    /** Dara brütten küçük değil; okuma hatalı. */
    WEIGHT_INCONSISTENT,
    /** Fişte basılı net, brüt eksi daraya uymuyor; alanlar karışmış. */
    ARITHMETIC_MISMATCH,
    /** Fiş tarihi yükleme zamanından 48 saatten fazla sapıyor; okuma yine geçerlidir. */
    DATE_OUT_OF_RANGE,
    BLURRY,
    GLARE,
    CROPPED,
    NOT_A_TICKET,
    /** Görüntü biçimi okunamıyor (ör. HEIC). */
    UNSUPPORTED_FORMAT;

    private static final Set<ReadingProblem> BLOCKING =
            Set.of(WEIGHT_NOT_READ, WEIGHT_INCONSISTENT, ARITHMETIC_MISMATCH);

    /** Bu sorun varken kilo güvenilmez; kullanıcıdan yeniden çekim istenir. */
    public boolean blocksReading() {
        return BLOCKING.contains(this);
    }
}
