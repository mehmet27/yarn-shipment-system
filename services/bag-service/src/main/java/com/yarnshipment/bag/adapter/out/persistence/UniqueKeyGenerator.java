package com.yarnshipment.bag.adapter.out.persistence;

import com.yarnshipment.bag.domain.model.PackageType;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Çuval Unique Key'i, {@code BAG-7K3M-Q9XD} biçiminde: sekiz karakter Crockford base32 (0-9, A-Z; I, L, O, U yok).
 * 40 bit rastgelelik sıradaki anahtarı tahmin edip sahte etiket basmayı pratik olmaktan çıkarır; karışan harfler
 * olmadığı için elle girişte de hata azalır. Tekillik veritabanındaki unique kısıtla ayrıca garanti edilir.
 */
@Component
final class UniqueKeyGenerator {

    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final int GROUP_LENGTH = 4;
    private static final int GROUP_COUNT = 2;

    private final SecureRandom random = new SecureRandom();

    /** Anahtarın öneki ambalaj türünü söyler (BAG-); etiket okunmadan da tür ayırt edilir. */
    String next(PackageType packageType) {
        StringBuilder key = new StringBuilder(prefix(packageType));
        for (int group = 0; group < GROUP_COUNT; group++) {
            if (group > 0) {
                key.append('-');
            }
            for (int index = 0; index < GROUP_LENGTH; index++) {
                key.append(ALPHABET[random.nextInt(ALPHABET.length)]);
            }
        }
        return key.toString();
    }

    static String prefix(PackageType packageType) {
        return switch (packageType) {
            case BAG -> "BAG-";
        };
    }
}
