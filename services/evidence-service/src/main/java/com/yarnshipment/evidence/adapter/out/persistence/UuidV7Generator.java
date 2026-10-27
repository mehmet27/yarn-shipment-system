package com.yarnshipment.evidence.adapter.out.persistence;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** RFC 9562 UUIDv7: ilk 48 bit milisaniye zaman damgası, kalanı rastgele. İndekste ekleme sırasını korur. */
@Component
final class UuidV7Generator {

    private static final int TIMESTAMP_SHIFT = 16;
    private static final long VERSION_7 = 0x7000L;
    private static final long RANDOM_12_BITS = 0x0FFFL;
    private static final long VARIANT_MASK = 0x3FFF_FFFF_FFFF_FFFFL;
    private static final long VARIANT_RFC = 0x8000_0000_0000_0000L;

    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    UuidV7Generator(Clock clock) {
        this.clock = clock;
    }

    UUID next() {
        long mostSignificant = (clock.millis() << TIMESTAMP_SHIFT) | VERSION_7 | (random.nextLong() & RANDOM_12_BITS);
        long leastSignificant = (random.nextLong() & VARIANT_MASK) | VARIANT_RFC;
        return new UUID(mostSignificant, leastSignificant);
    }
}
