package com.yarnshipment.discrepancy.domain.claim;

/** İtiraz için istenebilen kontroller (veritabanındaki {@code inspection.kind} değerlerinin itiraza açık olanları). */
public enum InspectionKind {
    /** Laboratuvar testi; sonuç belge olarak yüklenir. */
    LAB
}
