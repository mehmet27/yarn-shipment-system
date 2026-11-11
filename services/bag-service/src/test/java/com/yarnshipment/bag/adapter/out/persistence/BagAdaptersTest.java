package com.yarnshipment.bag.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.bag.domain.model.Bag;
import com.yarnshipment.bag.domain.model.BagStatus;
import java.util.List;
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

/** Çuval okuma, gerçek PostgreSQL'de ve yerel örnek veri üzerinde. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({BagQueryAdapter.class, UuidV7Generator.class})
class BagAdaptersTest {

    private static final PostgresTestDatabase DATABASE = PostgresTestDatabase.startIsolated();

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private BagQueryAdapter queries;

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
}
