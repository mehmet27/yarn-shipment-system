package com.yarnshipment.evidence.domain.model;

/** Kanıtın yüklendiği aşama; hangi izinle yüklenebileceğini belirler. */
public enum UploadStage {
    DISPATCH,
    DRIVER,
    DELIVERY,
    CLAIM
}
