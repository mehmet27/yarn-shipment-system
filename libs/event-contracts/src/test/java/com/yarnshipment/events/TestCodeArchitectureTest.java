package com.yarnshipment.events;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.yarnshipment.common.test.architecture.TestRules;

/** Test metotları NAME-35'e uyar. */
@AnalyzeClasses(packages = "com.yarnshipment.events")
class TestCodeArchitectureTest {

    @ArchTest
    static final ArchTests TEST_RULES = ArchTests.in(TestRules.class);
}
