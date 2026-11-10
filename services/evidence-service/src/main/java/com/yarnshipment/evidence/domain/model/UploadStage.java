package com.yarnshipment.evidence.domain.model;

/** Kanıtın yüklendiği aşama; hangi izinle yüklenebileceğini belirler. */
public enum UploadStage {
    /** Depo çıkış tartımı. */
    DISPATCH,
    /** Müdürün sevkiyatı tamamlaması (irsaliye). */
    COMPLETION,
    CLAIM
}
