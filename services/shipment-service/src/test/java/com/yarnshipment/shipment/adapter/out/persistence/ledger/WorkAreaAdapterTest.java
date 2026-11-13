package com.yarnshipment.shipment.adapter.out.persistence.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.shipment.adapter.out.persistence.PostgresTestDatabase;
import com.yarnshipment.shipment.domain.stock.WorkArea;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Çalışma alanı gerçek PostgreSQL'den kullanıcıya göre okunur. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(WorkAreaAdapter.class)
class WorkAreaAdapterTest {

    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-000000000902");

    @Autowired
    private WorkAreaAdapter adapter;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @Test
    @DisplayName("Çalışma alanı kullanıcıya göre okunur; kaydı olmayan kullanıcı için boş döner")
    void shouldReadWorkAreaByUser() {
        jdbc.update("INSERT INTO staff_work_area (user_id, work_area) VALUES (?, 'CENTRAL')", USER);

        assertThat(adapter.findByUser(USER)).contains(WorkArea.CENTRAL);
        assertThat(adapter.findByUser(UUID.randomUUID())).isEmpty();
    }
}
