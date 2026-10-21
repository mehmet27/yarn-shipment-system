package com.yarnshipment.fixture.structure;

import java.time.Instant;

/** STRUC-02, STRUC-08 ve DOM-10 kasıtlı ihlalleri. */
public class WeighingClock {

    public Instant weighedAt(boolean corrected) {
        return corrected ? Instant.EPOCH : Instant.now();
    }
}
