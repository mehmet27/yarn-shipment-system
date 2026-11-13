package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.shipment.adapter.out.persistence.PostgresTestDatabase;
import com.yarnshipment.shipment.masterdata.domain.model.StaffWorkArea;
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

/** Çalışma alanı kaydı gerçek PostgreSQL ve uygulama rolünün yetkileriyle yazılır; ikinci yazım alanı değiştirir. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(StaffWorkAreaAdapter.class)
class StaffWorkAreaAdapterTest {

    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-000000000901");

    @Autowired
    private StaffWorkAreaAdapter adapter;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @Test
    @DisplayName("Alan yazılır, aynı kullanıcı için tekrar yazılınca değişir")
    void shouldUpsertWorkArea() {
        adapter.save(USER, StaffWorkArea.CENTRAL);
        adapter.save(USER, StaffWorkArea.SUBCONTRACTOR);

        assertThat(jdbc.queryForObject("SELECT work_area FROM staff_work_area WHERE user_id = ?", String.class, USER))
                .isEqualTo("SUBCONTRACTOR");
    }

    @Test
    @DisplayName("Alan silinir ve bütün alanlar kullanıcıya göre okunur")
    void shouldDeleteAndListWorkAreas() {
        UUID other = UUID.fromString("0192f0a0-0000-7000-8000-000000000902");
        adapter.save(USER, StaffWorkArea.CENTRAL);
        adapter.save(other, StaffWorkArea.SUBCONTRACTOR);

        adapter.delete(USER);

        assertThat(adapter.findAll())
                .containsEntry(other, StaffWorkArea.SUBCONTRACTOR)
                .doesNotContainKey(USER);
    }
}
