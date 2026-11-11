package com.yarnshipment.reporting.domain.model;

/** İçe aktarma özeti: yazılan ve (zaten açılışı olduğu için) atlanan satırlar. */
public record OpeningImportResult(int imported, int skipped) {}
