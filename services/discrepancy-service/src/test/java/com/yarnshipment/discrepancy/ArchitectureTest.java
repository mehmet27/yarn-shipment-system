package com.yarnshipment.discrepancy;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.yarnshipment.common.test.architecture.HexagonalArchitectureRules;
import com.yarnshipment.common.test.architecture.NamingRules;
import com.yarnshipment.common.test.architecture.StructureRules;

/** Servis kodu ortak mimari kurallara uyar (Ek C.3). */
@AnalyzeClasses(packages = "com.yarnshipment.discrepancy", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchTests HEXAGONAL = ArchTests.in(HexagonalArchitectureRules.class);

    @ArchTest
    static final ArchTests NAMING = ArchTests.in(NamingRules.class);

    @ArchTest
    static final ArchTests STRUCTURE = ArchTests.in(StructureRules.class);
}
