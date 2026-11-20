package com.yarnshipment.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.common.web.platform.AuditActors;
import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.support.TransactionTemplate;

/** Gerçek PostgreSQL'e karşı: aktör ayarı trigger'ın okuduğu yerde durur ve transaction ile biter. */
class AuditActorsTest {

    private static final String CURRENT_ACTOR = "SELECT coalesce(current_setting('app.actor_id', true), '')";
    private static final UUID ACTOR_ID = UUID.fromString("0192b4a0-0000-7000-8000-000000000001");

    /** Havuzdaki bağlantının yeniden kullanılmasını taklit eder: iki transaction aynı fiziksel bağlantıda yürür. */
    private SingleConnectionDataSource pooled;

    private JdbcClient jdbc;
    private TransactionTemplate transaction;

    @BeforeEach
    void openConnection() throws SQLException {
        pooled =
                new SingleConnectionDataSource(PostgresTestDatabase.dataSource().getConnection(), true);
        jdbc = JdbcClient.create(pooled);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(pooled));
    }

    @AfterEach
    void closeConnection() {
        pooled.destroy();
    }

    private String currentActor() {
        return jdbc.sql(CURRENT_ACTOR).query(String.class).single();
    }

    @Test
    @DisplayName("Aktör, aynı transaction'daki denetim trigger'ının okuduğu ayara yazılır")
    void shouldExposeActorInsideTransaction() {
        String seen = transaction.execute(status -> {
            AuditActors.actAs(jdbc, ACTOR_ID);
            return currentActor();
        });

        assertThat(seen).isEqualTo(ACTOR_ID.toString());
    }

    @Test
    @DisplayName("Transaction bitince aktör ayarı silinir, sonraki işleme sızmaz")
    void shouldClearActorWhenTransactionEnds() {
        transaction.executeWithoutResult(status -> AuditActors.actAs(jdbc, ACTOR_ID));

        String seen = transaction.execute(status -> currentActor());

        assertThat(seen).isEmpty();
    }
}
