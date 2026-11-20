package com.yarnshipment.shipment.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.common.events.testing.RecordingDomainEventPublisher;
import com.yarnshipment.shipment.domain.bag.BagAssignment;
import com.yarnshipment.shipment.domain.model.DispatchBag;
import com.yarnshipment.shipment.domain.model.Shipment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Aynı sevkiyata aynı anda yazan istekler (CONC-07): her biri kendi transaction'ında, gerçek PostgreSQL'e karşı.
 * Satır kilidi olmadan iki istek aynı "son sıra numarasını" okur ve biri benzersizlik hatasıyla düşer.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    RecordingDomainEventPublisher.class,
    ShipmentPersistenceAdapter.class,
    BagAssignmentAdapter.class,
    ShipmentBagsAdapter.class,
    ShipmentTransitionWriter.class,
    UuidV7Generator.class
})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
class ShipmentRowLockTest {

    private static final PostgresTestDatabase DATABASE = PostgresTestDatabase.startIsolated();
    private static final UUID DRAFT = UUID.fromString("0192f0a0-0000-7000-8000-0000000a0001");
    private static final UUID ACTOR = UUID.fromString("0192f0a0-0000-7000-8000-000000000001");
    private static final Instant NOW = Instant.parse("2026-10-22T10:00:00Z");
    private static final int REQUESTS = 12;
    private static final int WAIT_SECONDS = 60;

    @Autowired
    private ShipmentPersistenceAdapter shipments;

    @Autowired
    private BagAssignmentAdapter assignments;

    @Autowired
    private ShipmentBagsAdapter bags;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        DATABASE.registerIn(registry);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/seed-local");
    }

    @Test
    @DisplayName("Aynı anda gelen çuval eklemeleri sıraya girer: hiçbiri düşmez, sıra numaraları 1..N kesintisizdir")
    void shouldSerializeConcurrentBagAssignments() throws Exception {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < REQUESTS; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    return transaction.execute(status -> {
                        assertThat(shipments.findShipmentForUpdate(DRAFT)).isPresent();
                        BagAssignment one = new BagAssignment(DRAFT, List.of(new BigDecimal("25.000")));
                        return assignments.assign(one, ACTOR, NOW);
                    });
                }));
            }
            start.countDown();
            for (Future<Integer> result : results) {
                assertThat(result.get(WAIT_SECONDS, TimeUnit.SECONDS)).isEqualTo(1);
            }
        }

        assertThat(bags.findDispatchBags(DRAFT))
                .extracting(DispatchBag::seqNo)
                .containsExactlyElementsOf(
                        IntStream.rangeClosed(1, REQUESTS).boxed().toList());
    }

    @Test
    @DisplayName("Olmayan sevkiyat kilitlenmez, boş döner")
    void shouldReturnEmptyForUnknownShipment() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        Optional<Shipment> found = transaction.execute(status -> shipments.findShipmentForUpdate(UUID.randomUUID()));

        assertThat(found).isEmpty();
    }
}
