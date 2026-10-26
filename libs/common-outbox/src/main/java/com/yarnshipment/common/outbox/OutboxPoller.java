package com.yarnshipment.common.outbox;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionException;

/**
 * Yayıncıyı uygulama açıkken düzenli aralıkla çalıştırır. Dolu bir tur sonrası beklemeden devam eder; birikmiş
 * olaylar böylece hızla erir.
 */
public final class OutboxPoller implements SmartLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(OutboxPoller.class);

    private final OutboxRelay relay;
    private final Duration interval;
    private @Nullable ScheduledExecutorService executor;
    private @Nullable ScheduledFuture<?> task;

    public OutboxPoller(OutboxRelay relay, Duration interval) {
        this.relay = relay;
        this.interval = interval;
    }

    @Override
    public synchronized void start() {
        ScheduledExecutorService started = Executors.newSingleThreadScheduledExecutor(
                Thread.ofVirtual().name("outbox-relay").factory());
        executor = started;
        task = started.scheduleWithFixedDelay(this::drain, 0, interval.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public synchronized void stop() {
        ScheduledFuture<?> scheduled = task;
        if (scheduled != null) {
            scheduled.cancel(true);
        }
        task = null;
        ScheduledExecutorService running = executor;
        if (running != null) {
            running.shutdownNow();
        }
        executor = null;
    }

    @Override
    public synchronized boolean isRunning() {
        return executor != null;
    }

    /** Zamanlanmış görev istisnayla biterse bir daha çalışmaz; veritabanı hatası da yalnızca loglanır. */
    private void drain() {
        try {
            int published;
            do {
                published = relay.publishPending();
            } while (published > 0 && !Thread.currentThread().isInterrupted());
        } catch (DataAccessException | TransactionException e) {
            LOG.warn("Outbox poll failed, will retry", e);
        }
    }
}
