package com.yarnshipment.common.events;

import java.security.SecureRandom;
import java.util.UUID;

/** RFC 9562 UUIDv7: ilk 48 bit milisaniye zaman damgası, kalanı rastgele. İndekste ekleme sırasını korur. */
final class UuidV7 {

    private static final int TIMESTAMP_SHIFT = 16;
    private static final long VERSION_7 = 0x7000L;
    private static final long RANDOM_12_BITS = 0x0FFFL;
    private static final long VARIANT_MASK = 0x3FFF_FFFF_FFFF_FFFFL;
    private static final long VARIANT_RFC = 0x8000_0000_0000_0000L;
    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {}

    static UUID next() {
        long mostSignificant =
                (System.currentTimeMillis() << TIMESTAMP_SHIFT) | VERSION_7 | (RANDOM.nextLong() & RANDOM_12_BITS);
        long leastSignificant = (RANDOM.nextLong() & VARIANT_MASK) | VARIANT_RFC;
        return new UUID(mostSignificant, leastSignificant);
    }
}
