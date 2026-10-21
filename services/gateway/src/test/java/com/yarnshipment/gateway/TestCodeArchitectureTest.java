package com.yarnshipment.gateway;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.yarnshipment.common.test.architecture.TestRules;

/** Test metotları NAME-35'e uyar. */
@AnalyzeClasses(packages = "com.yarnshipment.gateway")
class TestCodeArchitectureTest {

    @ArchTest
    static final ArchTests TEST_RULES = ArchTests.in(TestRules.class);
}
