package com.yarnshipment.bag.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.bag.application.port.out.BagReservationPort;
import com.yarnshipment.bag.domain.model.Bag;
import com.yarnshipment.bag.domain.model.BagAlreadyReservedException;
import com.yarnshipment.bag.domain.model.BagNotAvailableException;
import com.yarnshipment.bag.domain.model.BagStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Çuval okuma ve rezervasyon, gerçek PostgreSQL'de ve yerel örnek veri üzerinde. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({BagQueryAdapter.class, BagReservationAdapter.class, UuidV7Generator.class})
class BagAdaptersTest {

    private static final PostgresTestDatabase DATABASE = PostgresTestDatabase.startIsolated();
    private static final UUID ACTOR = UUID.fromString("0192f0a0-0000-7000-8000-000000000001");
    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-0000000009a1");
    private static final UUID OTHER_SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-0000000009a2");
    private static final Instant NOW = Instant.parse("2026-10-22T10:00:00Z");

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private BagQueryAdapter queries;

    @Autowired
    private BagReservationAdapter reservations;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        DATABASE.registerIn(registry);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/seed-local");
    }

    @Test
    @DisplayName("Çuval Unique Key ile okunur; ayrılmamış çuvalda sevkiyat yoktur")
    void shouldFindBagByUniqueKey() {
        Bag bag = queries.findByUniqueKey("BAG-0001").orElseThrow();

        assertThat(bag.status()).isEqualTo(BagStatus.CREATED);
        assertThat(bag.reservedShipmentId()).isNull();
        assertThat(queries.findByUniqueKey("YOK")).isEmpty();
        assertThat(queries.findByUniqueKeys(List.of())).isEmpty();
    }

    @Test
    @DisplayName("Rezervasyon çuvalı RESERVED yapar ve sevkiyatla birlikte okunur")
    void shouldReserveBags() {
        List<Bag> bags = queries.findByUniqueKeys(List.of("BAG-0001", "BAG-0002"));

        reservations.reserve(SHIPMENT, bags, ACTOR, NOW);

        assertThat(queries.findByUniqueKeys(List.of("BAG-0001", "BAG-0002"))).allSatisfy(bag -> {
            assertThat(bag.status()).isEqualTo(BagStatus.RESERVED);
            assertThat(bag.reservedShipmentId()).isEqualTo(SHIPMENT);
        });
        assertThat(queries.findReservedFor(SHIPMENT)).hasSize(2);
    }

    @Test
    @DisplayName("Aynı sevkiyata tekrar ayırmak hata vermez ve değişiklik yapmaz")
    void shouldBeIdempotentForSameShipment() {
        List<Bag> bags = queries.findByUniqueKeys(List.of("BAG-0001"));
        reservations.reserve(SHIPMENT, bags, ACTOR, NOW);

        reservations.reserve(SHIPMENT, queries.findByUniqueKeys(List.of("BAG-0001")), ACTOR, NOW);

        assertThat(queries.findReservedFor(SHIPMENT)).hasSize(1);
    }

    @Test
    @DisplayName("Başka sevkiyattaki çuval ayrılamaz")
    void shouldRejectBagReservedElsewhere() {
        reservations.reserve(SHIPMENT, queries.findByUniqueKeys(List.of("BAG-0001")), ACTOR, NOW);
        List<Bag> contested = queries.findByUniqueKeys(List.of("BAG-0001", "BAG-0002"));

        assertThatThrownBy(() -> reservations.reserve(OTHER_SHIPMENT, contested, ACTOR, NOW))
                .isInstanceOf(BagAlreadyReservedException.class);
    }

    @Test
    @DisplayName("İptal edilmiş (VOID) çuval ayrılamaz")
    void shouldRejectUnavailableBag() {
        List<Bag> voided = queries.findByUniqueKeys(List.of("BAG-0004"));

        assertThatThrownBy(() -> reservations.reserve(SHIPMENT, voided, ACTOR, NOW))
                .isInstanceOf(BagNotAvailableException.class);
    }

    @Test
    @DisplayName("Bırakma çuvalı yeniden kullanılabilir yapar ve başka sevkiyat ayırabilir")
    void shouldReleaseAndAllowReuse() {
        reservations.reserve(SHIPMENT, queries.findByUniqueKeys(List.of("BAG-0003")), ACTOR, NOW);

        int released = reservations.release(SHIPMENT, "iptal", ACTOR, NOW);
        reservations.reserve(OTHER_SHIPMENT, queries.findByUniqueKeys(List.of("BAG-0003")), ACTOR, NOW);

        assertThat(released).isEqualTo(1);
        assertThat(queries.findReservedFor(SHIPMENT)).isEmpty();
        assertThat(queries.findReservedFor(OTHER_SHIPMENT)).hasSize(1);
        assertThat(reservations.release(SHIPMENT, "tekrar", ACTOR, NOW)).isZero();
    }

    @Test
    @DisplayName("Seçili bırakma yalnızca verilen çuvalı serbest bırakır, diğerleri rezerve kalır")
    void shouldReleaseOnlySelectedBags() {
        List<Bag> bags = queries.findByUniqueKeys(List.of("BAG-0001", "BAG-0002"));
        reservations.reserve(SHIPMENT, bags, ACTOR, NOW);

        int released = reservations.releaseBags(
                new BagReservationPort.SelectedRelease(
                        SHIPMENT, List.of(bags.getFirst().id()), "yanlış"),
                ACTOR,
                NOW);

        assertThat(released).isEqualTo(1);
        assertThat(queries.findReservedFor(SHIPMENT))
                .extracting(Bag::id)
                .containsExactly(bags.getLast().id());
        assertThat(reservations.releaseBags(
                        new BagReservationPort.SelectedRelease(SHIPMENT, List.of(), "boş"), ACTOR, NOW))
                .isZero();
    }
}
