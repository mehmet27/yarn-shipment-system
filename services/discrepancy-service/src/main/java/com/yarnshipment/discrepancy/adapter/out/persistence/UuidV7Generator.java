package com.yarnshipment.discrepancy.adapter.out.persistence;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * RFC 9562 UUIDv7: ilk 48 bit milisaniye zaman damgası, ardından 12 bit sayaç, kalanı rastgele. Aynı
 * milisaniyede üretilen kimlikler sayaçla artar (RFC 9562 §6.2, yöntem 1); böylece aynı anda yazılan
 * geçmiş kayıtları {@code ORDER BY occurred_at, id} ile ekleme sırasında okunur.
 */
@Component
final class UuidV7Generator {

    private static final int TIMESTAMP_SHIFT = 16;
    private static final long VERSION_7 = 0x7000L;
    private static final int MAX_SEQUENCE = 0x0FFF;
    /** Yeni milisaniyede sayaç rastgele başlar ama taşmaya pay bırakmak için üst yarıya girmez. */
    private static final int SEQUENCE_SEED_BOUND = 0x0800;

    private static final long VARIANT_MASK = 0x3FFF_FFFF_FFFF_FFFFL;
    private static final long VARIANT_RFC = 0x8000_0000_0000_0000L;

    private final SecureRandom random = new SecureRandom();
    private final Clock clock;
    private long lastMillis = Long.MIN_VALUE;
    private int sequence;

    UuidV7Generator(Clock clock) {
        this.clock = clock;
    }

    synchronized UUID next() {
        long millis = clock.millis();
        if (millis > lastMillis) {
            lastMillis = millis;
            sequence = random.nextInt(SEQUENCE_SEED_BOUND);
        } else if (sequence < MAX_SEQUENCE) {
            sequence++;
        } else {
            // Sayaç taştı ya da saat geri gitti: zaman damgası ileri alınarak sıra korunur.
            lastMillis++;
            sequence = 0;
        }
        long mostSignificant = (lastMillis << TIMESTAMP_SHIFT) | VERSION_7 | sequence;
        long leastSignificant = (random.nextLong() & VARIANT_MASK) | VARIANT_RFC;
        return new UUID(mostSignificant, leastSignificant);
    }
}
