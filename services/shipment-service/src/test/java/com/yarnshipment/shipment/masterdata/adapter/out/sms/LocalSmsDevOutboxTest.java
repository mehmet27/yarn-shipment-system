package com.yarnshipment.shipment.masterdata.adapter.out.sms;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.shipment.adapter.out.persistence.PostgresTestDatabase;
import com.yarnshipment.shipment.masterdata.domain.model.PhoneNumber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/** Migration + uygulama rolünün GRANT'ı gerçek PostgreSQL'de: mesaj telefon maskeli, kod düz yazılır. */
@DataJpaTest(properties = {"yarnshipment.sms.provider=log", "spring.profiles.active=local"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(LocalSmsDevOutbox.class)
@Transactional
class LocalSmsDevOutboxTest {

    private static final PostgresTestDatabase DATABASE = PostgresTestDatabase.startIsolated();

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private LocalSmsDevOutbox outbox;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        DATABASE.registerIn(registry);
    }

    @Test
    @DisplayName("Kod tabloda görünür, telefon maskelidir")
    void shouldStoreCodeAndMaskedPhone() {
        outbox.record(new PhoneNumber("+905321234567"), "Şifre kodunuz: 123456");

        var row = jdbc.queryForMap("SELECT phone, message FROM sms_dev_outbox");
        assertThat(row.get("phone")).isEqualTo("***4567");
        assertThat((String) row.get("message")).contains("123456");
    }
}
