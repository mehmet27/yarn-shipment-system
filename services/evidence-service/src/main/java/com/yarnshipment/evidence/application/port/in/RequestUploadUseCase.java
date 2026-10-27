package com.yarnshipment.evidence.application.port.in;

/** Kanıt yükleme isteğini kabul eder ve imzalı yükleme adresi verir. */
public interface RequestUploadUseCase {

    UploadTicket requestUpload(RequestUploadCommand command);
}
