package com.yarnshipment.common.test.architecture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.lang.ArchRule;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HexagonalArchitectureRulesTest {

    private static final List<ArchRule> ALL_RULES = List.of(
            HexagonalArchitectureRules.DOMAIN_IS_FRAMEWORK_FREE,
            HexagonalArchitectureRules.DOMAIN_DOES_NOT_DEPEND_ON_OUTER_LAYERS,
            HexagonalArchitectureRules.APPLICATION_DOES_NOT_DEPEND_ON_ADAPTERS,
            HexagonalArchitectureRules.APPLICATION_USES_ONLY_ALLOWED_SPRING_TYPES,
            HexagonalArchitectureRules.INBOUND_ADAPTERS_DO_NOT_SEE_OUTBOUND_ADAPTERS,
            HexagonalArchitectureRules.OUTBOUND_ADAPTERS_DO_NOT_SEE_INBOUND_ADAPTERS,
            HexagonalArchitectureRules.CONTROLLERS_CALL_ONLY_INBOUND_PORTS,
            HexagonalArchitectureRules.JPA_ENTITIES_STAY_IN_PERSISTENCE_ADAPTER,
            HexagonalArchitectureRules.WEB_DTOS_STAY_IN_WEB_ADAPTER,
            HexagonalArchitectureRules.PACKAGES_ARE_FREE_OF_CYCLES);

    @Test
    @DisplayName("Kurallara uyan örnek servis tüm katman kurallarından geçer")
    void shouldAcceptCleanHexagonalService() {
        // when / then
        ALL_RULES.forEach(
                rule -> assertThatCode(() -> rule.allowEmptyShould(true).check(FixtureClasses.CLEAN))
                        .as(rule.getDescription())
                        .doesNotThrowAnyException());
    }

    @Test
    @DisplayName("Domain katmanı adapter'a bağımlı olursa reddedilir")
    void shouldRejectDomainDependingOnAdapter() {
        // when / then
        assertThatThrownBy(() ->
                        HexagonalArchitectureRules.DOMAIN_DOES_NOT_DEPEND_ON_OUTER_LAYERS.check(FixtureClasses.LAYERED))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("LeakyShipment");
    }

    @Test
    @DisplayName("Web adapter'ı persistence adapter'ını doğrudan görürse reddedilir")
    void shouldRejectInboundAdapterSeeingOutboundAdapter() {
        // when / then
        assertThatThrownBy(() -> HexagonalArchitectureRules.INBOUND_ADAPTERS_DO_NOT_SEE_OUTBOUND_ADAPTERS.check(
                        FixtureClasses.LAYERED))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("LeakyController");
    }

    @Test
    @DisplayName("JPA entity'si persistence adapter'ı dışında kullanılırsa reddedilir")
    void shouldRejectJpaEntityLeakingOutOfPersistence() {
        // when / then
        assertThatThrownBy(() -> HexagonalArchitectureRules.JPA_ENTITIES_STAY_IN_PERSISTENCE_ADAPTER.check(
                        FixtureClasses.LAYERED))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("ShipmentJpaEntity");
    }
}
