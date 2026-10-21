package com.yarnshipment.common.test.architecture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.lang.ArchRule;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NamingRulesTest {

    @Test
    @DisplayName("Kurallara uyan örnek servis adlandırma kurallarından geçer")
    void shouldAcceptCleanNames() {
        // given
        List<ArchRule> rules = List.of(
                NamingRules.NO_VAGUE_CLASS_NAMES,
                NamingRules.EXCEPTIONS_END_WITH_EXCEPTION,
                NamingRules.NO_I_PREFIXED_INTERFACES,
                NamingRules.NO_VAGUE_METHOD_NAMES,
                NamingRules.USE_CASES_ARE_SINGLE_METHOD_INTERFACES);

        // when / then
        rules.forEach(rule -> assertThatCode(() -> rule.allowEmptyShould(true).check(FixtureClasses.CLEAN))
                .as(rule.getDescription())
                .doesNotThrowAnyException());
    }

    @Test
    @DisplayName("Manager gibi belirsiz sınıf adları reddedilir")
    void shouldRejectVagueClassName() {
        // when / then
        assertThatThrownBy(() -> NamingRules.NO_VAGUE_CLASS_NAMES.check(FixtureClasses.NAMING))
                .hasMessageContaining("ShipmentManager");
    }

    @Test
    @DisplayName("process gibi belirsiz metot adı reddedilir, toString gibi ezilen metotlar serbesttir")
    void shouldRejectVagueMethodNameButAllowOverrides() {
        // when / then
        assertThatThrownBy(() -> NamingRules.NO_VAGUE_METHOD_NAMES.check(FixtureClasses.NAMING))
                .hasMessageContaining("process()")
                .hasMessageNotContaining("toString");
    }

    @Test
    @DisplayName("Exception ile bitmeyen istisna sınıfı reddedilir")
    void shouldRejectExceptionWithoutSuffix() {
        // when / then
        assertThatThrownBy(() -> NamingRules.EXCEPTIONS_END_WITH_EXCEPTION.check(FixtureClasses.NAMING))
                .hasMessageContaining("ShipmentFailure");
    }

    @Test
    @DisplayName("I önekli arayüz reddedilir")
    void shouldRejectInterfaceWithLetterPrefix() {
        // when / then
        assertThatThrownBy(() -> NamingRules.NO_I_PREFIXED_INTERFACES.check(FixtureClasses.NAMING))
                .hasMessageContaining("IShipmentPort");
    }

    @Test
    @DisplayName("common dışında Abstract öneki olmayan soyut sınıf reddedilir")
    void shouldRejectAbstractClassOutsideCommon() {
        // when / then
        assertThatThrownBy(() -> NamingRules.ABSTRACT_CLASSES_ARE_COMMON_AND_PREFIXED.check(FixtureClasses.NAMING))
                .hasMessageContaining("ShipmentTemplate");
    }
}
