package com.yarnshipment.evidence.domain.model;

/** Kanıt kaydının durumu. Kayıt silinmez; yanlış yükleme {@link #INVALIDATED} olur (DOM-17). */
public enum EvidenceStatus {
    VALID,
    INVALIDATED
}
