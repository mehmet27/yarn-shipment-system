package com.yarnshipment.discrepancy.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.common.security.scope.AccessScope;
import com.yarnshipment.discrepancy.TestComparisons;
import com.yarnshipment.discrepancy.application.port.in.DiscrepancyPage;
import com.yarnshipment.discrepancy.application.port.in.ListDiscrepanciesQuery.Filter;
import com.yarnshipment.discrepancy.domain.model.Severity;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Uyuşmazlık listesi, migration'larla kurulan gerçek şemada ve uygulama rolüyle. */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ComparisonAdapter.class, DiscrepancyQueryAdapter.class, UuidV7Generator.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class DiscrepancyQueryAdapterTest {

    private static final int PAGE_SIZE = 20;
    private static final Filter NO_FILTER = new Filter(null, null, null, null);

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ComparisonAdapter comparisons;

    @Autowired
    private DiscrepancyQueryAdapter queries;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @BeforeEach
    void openTwoDiscrepancies() {
        SeededDiscrepancies.seed(comparisons);
    }

    @Test
    @DisplayName("Liste kapsama ve süzgeçlere uyar; vadesi aynı olanlarda Kırmızı önce gelir")
    void shouldListWithinScopeAndFilters() {
        AccessScope ownSite = AccessScope.restrictedTo(Set.of(SeededDiscrepancies.SITE), null, null);
        AccessScope otherSite = AccessScope.restrictedTo(Set.of(UUID.randomUUID()), null, null);

        DiscrepancyPage all = queries.findDiscrepancies(NO_FILTER, ownSite, 0, PAGE_SIZE);
        DiscrepancyPage red =
                queries.findDiscrepancies(new Filter(null, Severity.RED, null, null), ownSite, 0, PAGE_SIZE);

        assertThat(all.totalElements()).isEqualTo(2);
        assertThat(all.items().getFirst().basis().severity()).isEqualTo(Severity.RED);
        assertThat(red.items()).hasSize(1);
        assertThat(queries.findDiscrepancies(NO_FILTER, otherSite, 0, PAGE_SIZE).items())
                .isEmpty();
        assertThat(queries.findDiscrepancies(NO_FILTER, AccessScope.UNRESTRICTED, 0, PAGE_SIZE)
                        .totalElements())
                .isEqualTo(2);
    }

    @Test
    @DisplayName("Fasoncu kapsamı uyuşmazlıkları fasoncu kimliğiyle süzer; kayıt fasoncuyu taşır")
    void shouldFilterBySubcontractor() {
        AccessScope own =
                AccessScope.restrictedTo(Set.of(SeededDiscrepancies.SITE), TestComparisons.SUBCONTRACTOR, null);
        AccessScope other = AccessScope.restrictedTo(Set.of(SeededDiscrepancies.SITE), UUID.randomUUID(), null);

        DiscrepancyPage mine = queries.findDiscrepancies(NO_FILTER, own, 0, PAGE_SIZE);

        assertThat(mine.totalElements()).isEqualTo(2);
        assertThat(mine.items())
                .allSatisfy(item -> assertThat(item.subcontractorId()).isEqualTo(TestComparisons.SUBCONTRACTOR));
        assertThat(queries.findDiscrepancies(NO_FILTER, other, 0, PAGE_SIZE).items())
                .isEmpty();
    }
}
