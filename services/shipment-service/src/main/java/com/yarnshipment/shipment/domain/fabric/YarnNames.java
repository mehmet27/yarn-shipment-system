package com.yarnshipment.shipment.domain.fabric;

import java.util.Locale;

/** İplik adı karşılaştırması: baş/son boşluk ve büyük-küçük harf (İ/ı dahil) önemsizdir. */
public final class YarnNames {

    private static final Locale TURKISH = Locale.forLanguageTag("tr-TR");

    private YarnNames() {}

    public static boolean same(String left, String right) {
        String first = left.strip();
        String second = right.strip();
        return first.equalsIgnoreCase(second)
                || first.toLowerCase(Locale.ROOT).equals(second.toLowerCase(Locale.ROOT))
                || first.toLowerCase(TURKISH).equals(second.toLowerCase(TURKISH));
    }
}
