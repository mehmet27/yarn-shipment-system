package com.yarnshipment.discrepancy.domain.claim;

/** İtiraz için istenebilen kontroller (veritabanındaki {@code inspection.kind} değerlerinin itiraza açık olanları). */
public enum InspectionKind {
    /** Fasoncudaki numunenin fabrikadaki şahit numuneyle karşılaştırılması. */
    SAMPLE,
    /** Laboratuvar testi; sonuç belge olarak yüklenir. */
    LAB
}
