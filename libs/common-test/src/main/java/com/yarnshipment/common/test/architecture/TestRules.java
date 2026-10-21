package com.yarnshipment.common.test.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Test kodu kuralları (NAME-35). Bu kurallar test sınıflarını da içe aktaran ayrı bir {@code @AnalyzeClasses}
 * tanımıyla çalıştırılır.
 */
public final class TestRules {

    @ArchTest
    public static final ArchRule TEST_METHODS_START_WITH_SHOULD = methods()
            .that()
            .areAnnotatedWith("org.junit.jupiter.api.Test")
            .or()
            .areAnnotatedWith("org.junit.jupiter.params.ParameterizedTest")
            .should()
            .haveNameMatching("^should[A-Z][a-zA-Z0-9]*$")
            .andShould()
            .beAnnotatedWith(RuleConstants.DISPLAY_NAME)
            .because("NAME-35: test adı davranışı anlatır, Türkçe açıklama @DisplayName ile verilir");

    private TestRules() {}
}
