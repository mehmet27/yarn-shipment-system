package com.yarnshipment.shipment.masterdata.domain.model;

import java.util.regex.Pattern;

/**
 * SMS gönderilecek cep telefonu, E.164 biçiminde ({@code +905321234567}). Türkiye numarası {@code 0532 123 45 67},
 * {@code 5321234567} ya da {@code +90 532 123 45 67} yazılabilir; yurt dışı numarası {@code +} ile başlamalıdır.
 *
 * @param e164 {@code +} ve 8-15 rakam
 */
public record PhoneNumber(String e164) {

    private static final Pattern E164 = Pattern.compile("^\\+[1-9][0-9]{7,14}$");
    private static final Pattern TURKISH_MOBILE = Pattern.compile("^(?:90|0)?(5[0-9]{9})$");
    private static final int VISIBLE_DIGITS = 4;
    private static final Pattern SEPARATORS = Pattern.compile("[\\s().-]");

    public PhoneNumber {
        if (!E164.matcher(e164).matches()) {
            throw new InvalidMasterDataException("phone", "not a phone number");
        }
    }

    /** Kullanıcının yazdığı numarayı çözer; tanınmazsa {@link InvalidMasterDataException} (alan {@code phone}). */
    public static PhoneNumber parse(String raw) {
        String compact = SEPARATORS.matcher(raw).replaceAll("");
        if (compact.startsWith("+")) {
            return new PhoneNumber(compact);
        }
        var turkish = TURKISH_MOBILE.matcher(compact);
        if (turkish.matches()) {
            return new PhoneNumber("+90" + turkish.group(1));
        }
        throw new InvalidMasterDataException("phone", "not a phone number");
    }

    /** Listede ve loglarda numaranın yalnızca son dört hanesi görünür. */
    public String masked() {
        return "***" + e164.substring(e164.length() - VISIBLE_DIGITS);
    }

    @Override
    public String toString() {
        return "PhoneNumber[" + masked() + "]";
    }
}
