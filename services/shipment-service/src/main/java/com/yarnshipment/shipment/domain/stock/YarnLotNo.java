package com.yarnshipment.shipment.domain.stock;

import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Girişte yazılan parti numarası; Partiler'deki parti numarası kuralının aynısı: kenar boşlukları atılır, büyük harfe
 * çevrilir, harf veya rakamla başlar, en çok 40 karakterdir, yalnızca harf, rakam ve {@code + . _ / -} içerir.
 */
public final class YarnLotNo {

    public static final int MAX_LENGTH = 40;
    private static final Pattern CODE = Pattern.compile("[A-Z0-9][A-Z0-9+._/-]*");

    private YarnLotNo() {}

    public static String normalize(String value) {
        String code = value.strip().toUpperCase(Locale.ROOT);
        if (code.isEmpty() || code.length() > MAX_LENGTH || !CODE.matcher(code).matches()) {
            throw new InvalidLedgerInputException("lotNo");
        }
        return code;
    }
}
