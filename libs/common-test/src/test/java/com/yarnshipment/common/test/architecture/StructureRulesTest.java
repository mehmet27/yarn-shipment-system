package com.yarnshipment.common.test.architecture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.lang.ArchRule;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StructureRulesTest {

    @Test
    @DisplayName("Kurallara uyan örnek servis yapı kurallarından geçer")
    void shouldAcceptCleanStructure() {
        // given
        List<ArchRule> rules = List.of(
                StructureRules.CONCRETE_CLASSES_ARE_FINAL,
                StructureRules.NO_BOOLEAN_FLAG_PARAMETERS,
                StructureRules.PACKAGES_HAVE_AT_MOST_30_TYPES,
                StructureRules.TIME_COMES_FROM_INJECTED_CLOCK,
                StructureRules.PACKAGES_ARE_NULL_MARKED);

        // when / then
        rules.forEach(rule -> assertThatCode(() -> rule.allowEmptyShould(true).check(FixtureClasses.CLEAN))
                .as(rule.getDescription())
                .doesNotThrowAnyException());
    }

    @Test
    @DisplayName("final olmayan somut sınıf reddedilir")
    void shouldRejectNonFinalConcreteClass() {
        // when / then
        assertThatThrownBy(() -> StructureRules.CONCRETE_CLASSES_ARE_FINAL.check(FixtureClasses.STRUCTURE))
                .hasMessageContaining("WeighingClock");
    }

    @Test
    @DisplayName("public metotta boolean bayrak parametresi reddedilir")
    void shouldRejectBooleanFlagParameter() {
        // when / then
        assertThatThrownBy(() -> StructureRules.NO_BOOLEAN_FLAG_PARAMETERS.check(FixtureClasses.STRUCTURE))
                .hasMessageContaining("weighedAt(boolean)");
    }

    @Test
    @DisplayName("Instant.now() çağrısı reddedilir, zaman Clock'tan alınır")
    void shouldRejectSystemClockCall() {
        // when / then
        assertThatThrownBy(() -> StructureRules.TIME_COMES_FROM_INJECTED_CLOCK.check(FixtureClasses.STRUCTURE))
                .hasMessageContaining("Instant.now()");
    }

    @Test
    @DisplayName("@NullMarked package-info olmayan paket reddedilir")
    void shouldRejectPackageWithoutNullMarked() {
        // when / then
        assertThatThrownBy(() -> StructureRules.PACKAGES_ARE_NULL_MARKED.check(FixtureClasses.UNMARKED))
                .hasMessageContaining("com.yarnshipment.fixture.unmarked");
    }
}
