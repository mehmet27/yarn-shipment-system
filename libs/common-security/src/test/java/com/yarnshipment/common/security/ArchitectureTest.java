package com.yarnshipment.common.security;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.tngtech.archunit.lang.ArchRule;
import com.yarnshipment.common.test.architecture.HexagonalArchitectureRules;
import com.yarnshipment.common.test.architecture.NamingRules;
import com.yarnshipment.common.test.architecture.StructureRules;

/** Kütüphane kodu ortak mimari kurallara uyar (ARCH-08, NAME, STRUC). */
@AnalyzeClasses(packages = "com.yarnshipment.common.security", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchTests NAMING = ArchTests.in(NamingRules.class);

    @ArchTest
    static final ArchTests STRUCTURE = ArchTests.in(StructureRules.class);

    @ArchTest
    static final ArchRule NO_CYCLES = HexagonalArchitectureRules.PACKAGES_ARE_FREE_OF_CYCLES;
}
