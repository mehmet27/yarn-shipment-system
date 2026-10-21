package com.yarnshipment.fixture.testnaming;

import org.junit.jupiter.api.Test;

/** NAME-35 kasıtlı ihlali. Surefire adlandırmasına uymadığı için test olarak çalıştırılmaz. */
final class BadTestNamesFixture {

    @Test
    void testSomething() {
        // Yalnızca ArchUnit kuralının test verisidir; çalıştırılmaz.
    }
}
