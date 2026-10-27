package com.yarnshipment.discrepancy;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.yarnshipment.common.test.architecture.HexagonalArchitectureRules;
import com.yarnshipment.common.test.architecture.NamingRules;
import com.yarnshipment.common.test.architecture.StructureRules;

/** Servis kodu ortak mimari kurallara uyar (Ek C.3). Sözleşmeden üretilen kod kural dışıdır. */
@AnalyzeClasses(
        packages = "com.yarnshipment.discrepancy",
        importOptions = {ImportOption.DoNotIncludeTests.class, ArchitectureTest.ExcludeGeneratedApi.class})
class ArchitectureTest {

    @ArchTest
    static final ArchTests HEXAGONAL = ArchTests.in(HexagonalArchitectureRules.class);

    @ArchTest
    static final ArchTests NAMING = ArchTests.in(NamingRules.class);

    @ArchTest
    static final ArchTests STRUCTURE = ArchTests.in(StructureRules.class);

    /** openapi-generator çıktısı ({@code adapter.in.web.api}) elle yazılmaz, kurallarla denetlenmez. */
    static final class ExcludeGeneratedApi implements ImportOption {

        @Override
        public boolean includes(Location location) {
            return !location.contains("/com/yarnshipment/discrepancy/adapter/in/web/api/");
        }
    }
}
