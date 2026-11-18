package com.yarnshipment.assistant.application.port.in;

/** Yardım asistanının kullanıma hazır olup olmadığını söyler. */
public interface GetHelpStatusUseCase {

    boolean isAvailable();
}
