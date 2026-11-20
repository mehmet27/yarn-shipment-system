package com.yarnshipment.shipment.adapter.out.persistence.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.common.events.testing.RecordingDomainEventPublisher;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.adapter.out.persistence.PostgresTestDatabase;
import com.yarnshipment.shipment.application.port.in.stock.RecordCentralProductionUseCase.Command;
import com.yarnshipment.shipment.application.port.in.stock.RecordCentralProductionUseCase.YarnRequest;
import com.yarnshipment.shipment.application.port.out.ledger.LedgerEventPort;
import com.yarnshipment.shipment.application.service.CentralStockService;
import com.yarnshipment.shipment.domain.stock.CentralStockMovement;
import com.yarnshipment.shipment.domain.stock.InsufficientCentralStockException;
import com.yarnshipment.shipment.domain.stock.LotRequest;
import com.yarnshipment.shipment.domain.stock.ResolvedLot;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Merkez stoktan aynı anda düşen işlemler (CONC-07): her biri kendi transaction'ında, gerçek PostgreSQL'e karşı. Kilit
 * olmadan iki üretim de "stok yetiyor" görür ve stok birlikte eksiye düşer.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({RecordingDomainEventPublisher.class, CentralStockAdapter.class, YarnLotAdapter.class, LedgerEventAdapter.class
})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@SuppressWarnings({"checkstyle:ClassFanOutComplexity", "checkstyle:ClassDataAbstractionCoupling"})
class CentralStockConcurrencyTest {

    private static final PostgresTestDatabase DATABASE = PostgresTestDatabase.startIsolated();
    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final UUID GRADE = UUID.fromString("0192f0a0-0000-7000-8000-000000000211");
    private static final UUID PRODUCT = UUID.fromString("0192f0a0-0000-7000-8000-00000000c101");
    private static final UUID LOT_PRODUCT = UUID.fromString("0192f0a0-0000-7000-8000-00000000c102");
    private static final UUID SUPPLIER = UUID.fromString("0192f0a0-0000-7000-8000-00000000c103");
    private static final Instant AT = Instant.parse("2026-11-13T09:00:00Z");
    private static final String YARN = "Ne 20/1 Karde";
    private static final int REQUESTS = 8;
    private static final int WAIT_SECONDS = 60;
    private static final long HOLD_MILLIS = 400;

    @Autowired
    private CentralStockAdapter stock;

    @Autowired
    private YarnLotAdapter lots;

    @Autowired
    private LedgerEventPort events;

    @Autowired
    private Clock clock;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        DATABASE.registerIn(registry);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/seed-local");
    }

    @BeforeAll
    static void insertDefinitions(@Autowired JdbcTemplate jdbc) {
        String product = "INSERT INTO md_product (id, code, name, active, created_at, created_by)"
                + " VALUES (?, ?, ?, true, now(), ?)";
        jdbc.update(product, PRODUCT, "C-20", YARN, USER);
        jdbc.update(product, LOT_PRODUCT, "C-21", "Ne 24/1 Karde", USER);
        jdbc.update(
                "INSERT INTO md_yarn_supplier (id, name, active, created_at, created_by) VALUES (?, ?, true, now(), ?)",
                SUPPLIER,
                "Eşzamanlı İplik",
                USER);
    }

    @Test
    @DisplayName("Aynı anda gelen üretimlerden yalnızca stoğun yettiği kadarı yazılır; stok eksiye düşmez")
    void shouldNotOverdrawStockWithConcurrentProductions() throws Exception {
        // given: 100 kg stok; her üretim 60 kg iplik ister (firesiz), yalnızca biri sığar
        stock.saveReceipt(CentralStockMovement.receipt(
                new CentralStockMovement.Receipt(PRODUCT, YARN, new BigDecimal("100"), null), AT, USER));
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        // when
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < REQUESTS; i++) {
                results.add(executor.submit(() -> produceSixtyKg(start)));
            }
            start.countDown();
            int recorded = 0;
            for (Future<Boolean> result : results) {
                recorded += result.get(WAIT_SECONDS, TimeUnit.SECONDS) ? 1 : 0;
            }

            // then
            assertThat(recorded).isEqualTo(1);
        }
        assertThat(jdbc.queryForObject(
                        "SELECT sum(net_kg) FROM central_stock_movement WHERE product_id = ?",
                        BigDecimal.class,
                        PRODUCT))
                .isEqualByComparingTo("40");
    }

    @Test
    @DisplayName("Aynı yeni parti numarasıyla aynı anda gelen iki giriş tek parti açar; ikincisi hata almaz")
    void shouldOpenLotOnceForConcurrentReceipts() throws Exception {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        LotRequest request =
                new LotRequest(USER, Set.of(TestTokens.SITE_ID), LOT_PRODUCT, SUPPLIER, "ES-2026-001", GRADE);
        CountDownLatch firstInserted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // İlk giriş partiyi açar ama transaction'ı açık tutar; ikincisi tam bu aralıkta gelir.
            Future<ResolvedLot> first = executor.submit(() -> transaction.execute(status -> {
                ResolvedLot lot = lots.resolve(request);
                firstInserted.countDown();
                await(release);
                return lot;
            }));
            assertThat(firstInserted.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            Future<ResolvedLot> second = executor.submit(() -> transaction.execute(status -> lots.resolve(request)));
            Thread.sleep(HOLD_MILLIS);
            release.countDown();

            assertThat(second.get(WAIT_SECONDS, TimeUnit.SECONDS).id())
                    .isEqualTo(first.get(WAIT_SECONDS, TimeUnit.SECONDS).id());
        }
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM md_production_lot WHERE upper(lot_no) = 'ES-2026-001'", Long.class))
                .isEqualTo(1);
    }

    /** 60 kg iplik isteyen (firesiz) bir üretim; stok yetmediği için reddedilirse {@code false}. */
    private boolean produceSixtyKg(CountDownLatch start) {
        CentralStockService service = new CentralStockService(stock, events, clock);
        Command command = new Command(
                TestTokens.actor("warehouse"),
                "Süprem",
                BigDecimal.ZERO,
                null,
                null,
                List.of(new YarnRequest(PRODUCT, new BigDecimal("60"))));
        await(start);
        try {
            new TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> service.recordCentralProduction(command));
            return true;
        } catch (InsufficientCentralStockException rejected) {
            return false;
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("release timed out");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }
}
