package com.yarnshipment.shipment.masterdata.domain.catalog;

import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Kod kuralı (ürün kodu, kalite kodu, parti numarası): kenar boşlukları atılır, büyük harfe çevrilir; harf veya
 * rakamla başlar, en çok 40 karakterdir ve yalnızca harf, rakam ve {@code + . _ / -} içerir. Kod etikete ve çuvala
 * kopyalandığı için Türkçe harf kabul edilmez. Ön yüz aynı deseni kullanır.
 */
public final class MasterDataCode {

    public static final int MAX_LENGTH = 40;

    private static final Pattern CODE = Pattern.compile("[A-Z0-9][A-Z0-9+._/-]*");

    private MasterDataCode() {}

    public static String normalize(String field, String value) {
        String code = value.strip().toUpperCase(Locale.ROOT);
        if (code.isEmpty()) {
            throw new InvalidMasterDataException(field, "blank");
        }
        if (code.length() > MAX_LENGTH || !CODE.matcher(code).matches()) {
            throw new InvalidMasterDataException(field, "invalid code");
        }
        return code;
    }
}
