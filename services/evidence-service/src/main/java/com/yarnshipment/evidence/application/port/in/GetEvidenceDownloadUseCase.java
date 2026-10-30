package com.yarnshipment.evidence.application.port.in;

/** Kanıt dosyası için imzalı indirme adresi verir; görüntüleme kuralları üst veriyle aynıdır. */
public interface GetEvidenceDownloadUseCase {

    DownloadLink getDownloadLink(GetEvidenceQuery query);
}
