package com.yarnshipment.common.test.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;

/** Kural testlerinin kullandığı, kasıtlı ihlal içeren örnek paketler. */
final class FixtureClasses {

    static final JavaClasses CLEAN = importPackage("clean");
    static final JavaClasses LAYERED = importPackage("layered");
    static final JavaClasses NAMING = importPackage("naming");
    static final JavaClasses STRUCTURE = importPackage("structure");
    static final JavaClasses UNMARKED = importPackage("unmarked");
    static final JavaClasses TEST_NAMING = importPackage("testnaming");

    private FixtureClasses() {}

    private static JavaClasses importPackage(String name) {
        return new ClassFileImporter().importPackages("com.yarnshipment.fixture." + name);
    }
}
