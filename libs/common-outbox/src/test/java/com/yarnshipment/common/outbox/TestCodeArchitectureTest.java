package com.yarnshipment.common.outbox;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.yarnshipment.common.test.architecture.TestRules;

/** Test metotları NAME-35'e uyar. */
@AnalyzeClasses(packages = "com.yarnshipment.common.outbox")
class TestCodeArchitectureTest {

    @ArchTest
    static final ArchTests TEST_RULES = ArchTests.in(TestRules.class);
}
