package com.yarnshipment.assistant.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Kullanıcı başına soru sınırı: API bütçesini tek kişinin tüketmesini önler. Sayaç bellektedir, yeniden başlatmada
 * sıfırlanır.
 */
@Component
final class QuestionRateLimiter {

    static final int MAX_QUESTIONS = 30;
    static final Duration WINDOW = Duration.ofHours(1);

    private final Clock clock;
    private final Map<UUID, Deque<Instant>> asked = new ConcurrentHashMap<>();

    QuestionRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Soru hakkı varsa bir hak harcar ve true döner; sınır dolmuşsa false döner. */
    boolean tryAcquire(UUID userId) {
        Instant now = clock.instant();
        Instant windowStart = now.minus(WINDOW);
        Deque<Instant> times = asked.computeIfAbsent(userId, key -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst().isBefore(windowStart)) {
                times.removeFirst();
            }
            if (times.size() >= MAX_QUESTIONS) {
                return false;
            }
            times.addLast(now);
            return true;
        }
    }
}
