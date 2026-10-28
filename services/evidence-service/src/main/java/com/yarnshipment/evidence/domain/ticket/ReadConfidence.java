package com.yarnshipment.evidence.domain.ticket;

/** Modelin bir alanı ne kadar emin okuduğu. Yalnızca {@link #HIGH} kilo değeri kabul edilir. */
public enum ReadConfidence {
    HIGH,
    MEDIUM,
    LOW
}
