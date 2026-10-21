package com.yarnshipment.common.test.architecture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestRulesTest {

    @Test
    @DisplayName("should ile başlamayan ve @DisplayName taşımayan test metodu reddedilir")
    void shouldRejectTestWithoutShouldPrefixAndDisplayName() {
        // when / then
        assertThatThrownBy(() -> TestRules.TEST_METHODS_START_WITH_SHOULD.check(FixtureClasses.TEST_NAMING))
                .hasMessageContaining("testSomething");
    }

    @Test
    @DisplayName("Bu modülün kendi testleri test adlandırma kuralına uyar")
    void shouldAcceptOwnTests() {
        // given
        var ownTests = new ClassFileImporter().importPackages("com.yarnshipment.common.test.architecture");

        // when / then
        TestRules.TEST_METHODS_START_WITH_SHOULD.check(ownTests);
    }
}
