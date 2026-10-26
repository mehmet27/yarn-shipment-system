package com.yarnshipment.discrepancy.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.discrepancy.domain.model.Discrepancy;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyNotOpenException;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyStatus;
import com.yarnshipment.discrepancy.domain.model.Severity;
import com.yarnshipment.events.discrepancy.DiscrepancyResolved;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Uyuşmazlık okuma, atama ve karar yazımı; migration'larla kurulan gerçek şemada ve uygulama rolüyle. */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ComparisonAdapter.class, DiscrepancyQueryAdapter.class, DiscrepancyDecisionAdapter.class, UuidV7Generator.class
})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class DiscrepancyAdaptersTest {

    private static final UUID ACTOR = SeededDiscrepancies.ACTOR;
    private static final Instant LATER = Instant.parse("2026-10-24T09:00:00Z");

    @Autowired
    private ComparisonAdapter comparisons;

    @Autowired
    private DiscrepancyQueryAdapter queries;

    @Autowired
    private DiscrepancyDecisionAdapter decisions;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @BeforeEach
    void openTwoDiscrepancies() {
        SeededDiscrepancies.seed(comparisons);
    }

    @Test
    @DisplayName("Atama incelemeye alır ve geçmişe yazılır; karar kalan sayısıyla olay üretir")
    void shouldAssignAndResolve() {
        Discrepancy first = SeededDiscrepancies.find(queries, Severity.RED);
        Discrepancy second = SeededDiscrepancies.find(queries, Severity.YELLOW);

        decisions.saveAssignment(first.assignTo(ACTOR), ACTOR, LATER);
        decisions.saveResolution(first.assignTo(ACTOR).resolve(SeededDiscrepancies.decision()), ACTOR, LATER);
        decisions.saveResolution(second.resolve(SeededDiscrepancies.decision()), ACTOR, LATER);

        Discrepancy stored = queries.findDiscrepancy(first.id()).orElseThrow();
        assertThat(stored.status()).isEqualTo(DiscrepancyStatus.RESOLVED);
        assertThat(stored.decision()).isNotNull();
        assertThat(queries.findActivity(first.id()))
                .extracting(activity -> activity.actionType())
                .containsExactly("OPENED", "ASSIGNED", "RESOLVED");
        assertThat(remainingOpenFromEvents()).containsExactly(1, 0);
    }

    @Test
    @DisplayName("Zaten çözülmüş uyuşmazlığa ikinci karar yazılamaz")
    void shouldRejectSecondDecision() {
        Discrepancy red = SeededDiscrepancies.find(queries, Severity.RED);
        decisions.saveResolution(red.resolve(SeededDiscrepancies.decision()), ACTOR, LATER);

        assertThatThrownBy(() -> decisions.saveResolution(red.resolve(SeededDiscrepancies.decision()), ACTOR, LATER))
                .isInstanceOf(DiscrepancyNotOpenException.class);
        assertThatThrownBy(() -> decisions.saveAssignment(red.assignTo(ACTOR), ACTOR, LATER))
                .isInstanceOf(DiscrepancyNotOpenException.class);
    }

    private List<@Nullable Integer> remainingOpenFromEvents() {
        return jdbc.queryForList(
                "SELECT (payload ->> 'remainingOpen')::int FROM outbox_event WHERE event_type = ?"
                        + " ORDER BY created_at, id",
                Integer.class,
                DiscrepancyResolved.TYPE);
    }
}
