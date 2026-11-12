package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.shipment.adapter.out.persistence.PostgresTestDatabase;
import com.yarnshipment.shipment.masterdata.domain.branding.Branding;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Marka ayarı {@code app_setting} anahtarlarına yazılır; uygulama rolünün yetkileriyle ve gerçek PostgreSQL'de. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BrandingAdapterTest {

    private static final UUID ACTOR = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final String LOGO = "data:image/png;base64,iVBORw0KGgo=";

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private JdbcClient jdbc;

    private BrandingAdapter adapter;

    @BeforeEach
    void createAdapter() {
        adapter = new BrandingAdapter(jdbc, Clock.fixed(Instant.parse("2026-11-16T09:00:00Z"), ZoneOffset.UTC));
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @Test
    @DisplayName("Marka kaydedilmemişse boş döner")
    void shouldReturnEmptyWhenNothingStored() {
        assertThat(adapter.findBranding()).isEmpty();
    }

    @Test
    @DisplayName("Marka yazılır, okunur ve firma adı ile logo kaldırılabilir")
    void shouldStoreAndClearOptionalParts() {
        Branding full = new Branding("Örnek Tekstil", "#17527a", LOGO);
        assertThat(adapter.upsertBranding(ACTOR, full)).isEqualTo(full);

        Branding colorOnly = new Branding(null, "#ff0000", null);
        assertThat(adapter.upsertBranding(ACTOR, colorOnly)).isEqualTo(colorOnly);
        assertThat(adapter.findBranding()).contains(colorOnly);
    }
}
